package kr.henein.api.service;

import com.querydsl.jpa.impl.JPAQueryFactory;
import kr.henein.api.dto.captcha.CaptchaResponseDto;
import kr.henein.api.dto.login.BasicLoginRequestDto;
import kr.henein.api.dto.login.BasicRegisterRequestDto;
import kr.henein.api.dto.login.KakaoOAuth2User;
import kr.henein.api.entity.QAccountBanEntity;
import kr.henein.api.entity.QUserEntity;
import kr.henein.api.entity.UserEntity;
import kr.henein.api.enumCustom.UserRole;
import kr.henein.api.error.ErrorCode;
import kr.henein.api.error.exception.BadRequestException;
import kr.henein.api.error.exception.UnAuthorizedException;
import kr.henein.api.jwt.JwtTokenProvider;
import kr.henein.api.jwt.KakaoOAuth2AccessTokenResponse;
import kr.henein.api.jwt.KakaoOAuth2Client;
import kr.henein.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final KakaoOAuth2Client kakaoOAuth2Client;
    private final PasswordEncoder passwordEncoder;
    private final RedisService redisService;
    private final JPAQueryFactory jpaQueryFactory;

    @Value("${google.recaptcha.key.secret-key}")
    private String secretKey;
    @Value("${google.recaptcha.key.url}")
    private String url;

    //==================로그인 관련
    private UserEntity getUserEntity(String userEmail) {
        QUserEntity qUserEntity = QUserEntity.userEntity;
        QAccountBanEntity qAccountBanEntity = QAccountBanEntity.accountBanEntity;
        UserEntity userEntity = jpaQueryFactory
                .selectFrom(qUserEntity)
                .leftJoin(qUserEntity.accountBanEntity, qAccountBanEntity).fetchJoin()
                .where(qUserEntity.userEmail.eq(userEmail))
                .fetchOne();

        if (userEntity == null) {
            throw new UnAuthorizedException("cannot found account", ErrorCode.NOT_FOUND);
        }
        if (userEntity.getAccountBanEntity() != null) {
            throw new UnAuthorizedException("이 계정은 "+ userEntity.getAccountBanEntity().getFinPeriod()+"일까지 정지된 계정입니다.", ErrorCode.INVALID_ACCESS);
        }
        return userEntity;
    }


    @Transactional
    public ResponseEntity<?> refreshAT(HttpServletRequest request,HttpServletResponse response) {
        //bearer 지우기
        String RTHeader = jwtTokenProvider.resolveRefreshToken(request);

        // rt 넣어서 검증하고 유저이름 가져오기
        String userEmail = jwtTokenProvider.refreshAccessToken(RTHeader);
        UserEntity userEntity = this.getUserEntity(userEmail);


        //db에 있는 토큰값과 넘어온 토큰이 같은지
        if (!userEntity.getRefreshToken().equals(RTHeader)){
            throw new UnAuthorizedException(ErrorCode.EXPIRED_RT.getMessage(),ErrorCode.EXPIRED_RT);
        }
        String newAccessToken = jwtTokenProvider.generateAccessToken(userEmail, userEntity.getUserRole());

        response.setHeader("Authorization", "Bearer " + newAccessToken);

        return ResponseEntity.ok("good");
    }

    @Transactional
    public ResponseEntity<?> basicLogin(BasicLoginRequestDto basicLoginRequestDto, HttpServletResponse servletResponse){
        UserEntity userEntity = this.getUserEntity(basicLoginRequestDto.getUserEmail());

        if ( !passwordEncoder.matches(basicLoginRequestDto.getPassword(),userEntity.getPassword()) ) {
            throw new UnAuthorizedException("비밀번호가 틀렸습니다.",ErrorCode.INVALID_ACCESS);
        }

        return WebClient.builder()
                .baseUrl(url)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE) // 기본 헤더 설정
                .build()
                .post()
                .uri(uriBuilder -> uriBuilder
                        .queryParam("secret", secretKey)
                        .queryParam("response", basicLoginRequestDto.getCaptchaValue())
                        .build())
                .retrieve()
                .bodyToMono(CaptchaResponseDto.class)
                .map(response -> {
                    if( response.isSuccess() ){
                        String AT = jwtTokenProvider.generateAccessToken(userEntity.getUserEmail(), userEntity.getUserRole());
                        String RT = jwtTokenProvider.generateRefreshToken(userEntity.getUserEmail());
                        userEntity.setRefreshToken(RT);

                        servletResponse.setHeader("Authorization","Bearer " + AT);
                        servletResponse.setHeader("RefreshToken","Bearer "+ RT);
                        return ResponseEntity.ok().build();
                    } else {
                        String message = response.getErrorList().stream()
                                .map(Objects::toString)
                                .collect(Collectors.joining(", "));

                        throw new BadRequestException(ErrorCode.CAPTCHA_FAILED.getMessage()+message, ErrorCode.CAPTCHA_FAILED);
                    }
                })
                .block();

    }

    @Transactional
    public ResponseEntity<String> basicSignUp(BasicRegisterRequestDto basicRegisterRequestDto, HttpServletRequest request, HttpServletResponse response){

        String requestAT = jwtTokenProvider.resolveAccessToken(request);
        if ( !redisService.verifySignUpRequest(basicRegisterRequestDto.getUserEmail(), requestAT) ) {
            throw new UnAuthorizedException("Do not match email with AT", ErrorCode.JWT_COMPLEX_ERROR);
        }
        String AT = jwtTokenProvider.generateAccessToken(basicRegisterRequestDto.getUserEmail(), UserRole.USER);
        String RT = jwtTokenProvider.generateRefreshToken(basicRegisterRequestDto.getUserEmail());

        String uid = UUID.randomUUID().toString();

        UserEntity userEntity = UserEntity.builder()
                .userRole(UserRole.USER)
                .userName(uid)
                .refreshToken(RT)
                .userEmail(basicRegisterRequestDto.getUserEmail())
                .isAnonymous(true)
                .uid(uid)
                .password(passwordEncoder.encode(basicRegisterRequestDto.getPassword()))
                .build();
        userRepository.save(userEntity);

        response.setHeader("Authorization","Bearer " + AT);
        response.setHeader("RefreshToken","Bearer "+ RT);


        return ResponseEntity.ok("회원가입 성공");
    }
    @Transactional
    public ResponseEntity<?> kakaoLogin(String code, HttpServletResponse response) {

        KakaoOAuth2AccessTokenResponse tokenResponse = kakaoOAuth2Client.getAccessToken(code);
        // 카카오 사용자 정보를 가져옵니다.
        KakaoOAuth2User kakaoOAuth2User = kakaoOAuth2Client.getUserProfile(tokenResponse.getAccessToken());

        // 사용자 정보를 기반으로 우리 시스템에 인증을 수행합니다.
        Authentication authentication = new UsernamePasswordAuthenticationToken(kakaoOAuth2User, null);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        String userEmail = kakaoOAuth2User.getKakao_account().getEmail();
        String RT = jwtTokenProvider.generateRefreshToken(userEmail);


        QUserEntity qUserEntity = QUserEntity.userEntity;
        QAccountBanEntity qAccountBanEntity = QAccountBanEntity.accountBanEntity;
        UserEntity userEntity = jpaQueryFactory
                .selectFrom(qUserEntity)
                .leftJoin(qUserEntity.accountBanEntity, qAccountBanEntity).fetchJoin()
                .where(qUserEntity.userEmail.eq(userEmail))
                .fetchOne();

        if (userEntity == null) {
            userEntity = new UserEntity(userEmail);
        }
        if (userEntity.getAccountBanEntity() != null) {
            throw new UnAuthorizedException("이 계정은 "+ userEntity.getAccountBanEntity().getFinPeriod()+"일까지 정지된 계정입니다.", ErrorCode.INVALID_ACCESS);
        }

        //신규회원이면
        Map<String, String> tokens = new HashMap<>();
        if (userEntity.getRefreshToken()==null) {
            tokens.put("status","신규 유저입니다.");
            userEntity.setRefreshToken(RT);
            userRepository.save(userEntity);
        } else {
            userEntity.setRefreshToken(RT);
        }

        String AT = jwtTokenProvider.generateAccessToken(userEmail, userEntity.getUserRole());


        response.setHeader("Authorization","Bearer " + AT);
        response.setHeader("RefreshToken","Bearer " + RT);

        return ResponseEntity.ok(tokens);
    }
}
