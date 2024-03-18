package kr.henein.api.service;

import kr.henein.api.dto.board.BoardListResponseDto;
import kr.henein.api.dto.user.UserDetailInfoResponseDto;
import kr.henein.api.dto.user.UserInfoChange;
import kr.henein.api.dto.user.UserInfoResponseDto;
import kr.henein.api.entity.*;
import kr.henein.api.enumCustom.S3EntityType;
import kr.henein.api.error.ErrorCode;
import kr.henein.api.error.exception.BadRequestException;
import kr.henein.api.error.exception.ForbiddenException;
import kr.henein.api.error.exception.NotFoundException;
import kr.henein.api.jwt.JwtTokenProvider;
import kr.henein.api.repository.S3FileRepository;
import kr.henein.api.repository.UserCharRepository;
import kr.henein.api.repository.UserRepository;
import com.querydsl.jpa.impl.JPAQueryFactory;
import kr.henein.api.dto.userchar.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;
import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final S3FileRepository s3FileRepository;
    private final S3Service s3Service;
    private final UserCharRepository userCharRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final WebClient APIClient;
    private final RedisService redisService;
    private final JPAQueryFactory jpaQueryFactory;

    @Value("${apiKey}")
    private String apiKey;

    //===============마이페이지 관련===================

    public UserInfoResponseDto userInfo(HttpServletRequest request){
        String userEmail = jwtTokenProvider.fetchUserEmailByHttpRequest(request);
        UserEntity userEntity = userRepository.findByUserEmail(userEmail).orElseThrow(()->{throw new NotFoundException(ErrorCode.NOT_FOUND_EXCEPTION.getMessage(), ErrorCode.NOT_FOUND_EXCEPTION);});


        UserCharEntity userCharEntity = userCharRepository.findByUserEntityAndPickByUser(userEntity,true);

        List<S3File> s3File = s3FileRepository.findAllByS3EntityTypeAndTypeId(S3EntityType.USER,userEntity.getId());

        if (s3File.isEmpty() && userCharEntity == null ) {
            return new UserInfoResponseDto(userEntity,null,null);
        }
        else if (userCharEntity == null) {
            return new UserInfoResponseDto(userEntity,null,s3File.get(0).getFileUrl());
        }
        else if (s3File.isEmpty()) {
            return new UserInfoResponseDto(userEntity,userCharEntity.getCharName(),null);
        }

        return new UserInfoResponseDto(userEntity,userCharEntity.getCharName(),s3File.get(0).getFileUrl());
    }

    public UserDetailInfoResponseDto userDetailInfo(HttpServletRequest request) {
        String userEmail = jwtTokenProvider.fetchUserEmailByHttpRequest(request);
        UserEntity userEntity = userRepository.findByUserEmail(userEmail).orElseThrow(()->{throw new NotFoundException(ErrorCode.NOT_FOUND_EXCEPTION.getMessage(), ErrorCode.NOT_FOUND_EXCEPTION);});


        QBoardEntity qBoardEntity= QBoardEntity.boardEntity;
        long boardCount = jpaQueryFactory
                .selectFrom(qBoardEntity)
                .where(qBoardEntity.userEntity.eq(userEntity))
                .fetchCount();

        QCommentEntity qCommentEntity = QCommentEntity.commentEntity;
        long commentCount = jpaQueryFactory
                .selectFrom(qCommentEntity)
                .where(qCommentEntity.userEmail.eq(userEntity.getUserEmail()))
                .fetchCount();

        List<S3File> s3File = s3FileRepository.findAllByS3EntityTypeAndTypeId(S3EntityType.USER,userEntity.getId());

        if (s3File.isEmpty()) {
            return new UserDetailInfoResponseDto(userEntity,null,boardCount,commentCount);
        }
        return new UserDetailInfoResponseDto(userEntity,s3File.get(0).getFileUrl(),boardCount,commentCount);
    }
    @Transactional
    public String userUpdate(UserInfoChange userInfoChange, HttpServletRequest request) throws IOException {
        String userEmail = jwtTokenProvider.fetchUserEmailByHttpRequest(request);
        UserEntity userEntity = userRepository.findByUserEmail(userEmail).orElseThrow(()->{throw new NotFoundException(ErrorCode.NOT_FOUND_EXCEPTION.getMessage(), ErrorCode.NOT_FOUND_EXCEPTION);});

        if (!userInfoChange.getUserName().trim().isEmpty()) {
            userEntity.updateUserName(userInfoChange.getUserName());
            userEntity.updateAnonymous(false);
        }
        if (!(userInfoChange.getImage() == null || userInfoChange.getImage().isEmpty())) {
            s3Service.uploadImageUserPicture(userInfoChange.getImage(), userEntity.getId());
        }

        return "200ok";
    }
    @Transactional
    public void changeToAnonymous(HttpServletRequest request) {
        String userEmail = jwtTokenProvider.fetchUserEmailByHttpRequest(request);
        UserEntity userEntity = userRepository.findByUserEmail(userEmail).orElseThrow(()->{throw new NotFoundException(ErrorCode.NOT_FOUND_EXCEPTION.getMessage(), ErrorCode.NOT_FOUND_EXCEPTION);});
        userEntity.updateUserName(userEntity.getUid());
        userEntity.updateAnonymous(true);
    }
    //=============================캐릭터 관련===================================================================================
    @Transactional
    public void pickCharacter(Long id, HttpServletRequest request) {
        String userEmail = jwtTokenProvider.fetchUserEmailByHttpRequest(request);
        UserEntity userEntity = userRepository.findByUserEmail(userEmail).orElseThrow(()->{throw new NotFoundException(ErrorCode.NOT_FOUND_EXCEPTION.getMessage(), ErrorCode.NOT_FOUND_EXCEPTION);});

        UserCharEntity oldCharEntity = userCharRepository.findByUserEntityAndPickByUser(userEntity, true);

        if (oldCharEntity == null){
            UserCharEntity newCharEntity = userCharRepository.findByUserEntityAndId(userEntity, id);
            newCharEntity.pickThisCharacter();
            return;
        }
        else if (oldCharEntity.getId().equals(id) ){
            oldCharEntity.unPickThisCharacter();
            return;
        }

        UserCharEntity newCharEntity = userCharRepository.findByUserEntityAndId(userEntity, id);

        oldCharEntity.unPickThisCharacter();
        newCharEntity.pickThisCharacter();

    }

    @Transactional
    public List<UserCharacterResponse> getAllUserCharacterInfo(HttpServletRequest request) {
        String userEmail= jwtTokenProvider.fetchUserEmailByHttpRequest(request);
        UserEntity userEntity = userRepository.findByUserEmail(userEmail).orElseThrow(()->{throw new NotFoundException(ErrorCode.NOT_FOUND_EXCEPTION.getMessage(), ErrorCode.NOT_FOUND_EXCEPTION);});

        List<UserCharEntity> resultList = userCharRepository.findAllByUserEntity(userEntity);
        return resultList.stream().map(UserCharacterResponse::new).collect(Collectors.toList());
    }

    @Transactional
    //인증 받아오기
    public Mono<String> requestToAPIServer(HttpServletRequest request, UserMapleApi userMapleApi){
        //날짜가 오늘일시 에러.
        if (userMapleApi.getRecentDay().equals(LocalDate.now())) {
            throw new BadRequestException("Today's date cannot be requested", ErrorCode.BAD_REQUEST);
        }
        else if (ChronoUnit.DAYS.between(userMapleApi.getRecentDay(),userMapleApi.getPastDay()) > 61){
            throw new BadRequestException("Cannot request for more than 2 months", ErrorCode.BAD_REQUEST);
        }
        //날짜 비교해서 2달 이상이면 에러
        String userEmail = jwtTokenProvider.fetchUserEmailByHttpRequest(request);
        UserEntity userEntity = userRepository.findByUserEmail(userEmail).orElseThrow(()->{throw new NotFoundException(ErrorCode.NOT_FOUND_EXCEPTION.getMessage(), ErrorCode.NOT_FOUND_EXCEPTION);});

        userEntity.UpdateApiKey(userMapleApi.getUserApi());
        String api = "/cube?key="+apiKey;

        return this.APIClient.post()
                .uri(api)
                .body(BodyInserters.fromValue(userMapleApi))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Set<String>>() {})
                .flatMap(result -> {
                    List<UserCharEntity> userCharEntityList = userCharRepository.findAllByUserEntity(userEntity);
                    log.info(String.valueOf(result.size()));
                    for ( String r : result) {
                        for (UserCharEntity u : userCharEntityList) {
                            if (r.equals(u.getCharName())) {
                                result.remove(r);
                                break;
                            }
                        }
                    }
                    List<UserCharEntity> newCharEntityList = new ArrayList<>();
                    for (String r : result) {
                        log.info("들어옴");
                        newCharEntityList.add(new UserCharEntity(userEntity, r));
                    }
                    if (!newCharEntityList.isEmpty()) {
                        log.info("들어옴2");
                        userCharRepository.saveAll(newCharEntityList);
                    }
                    return Mono.just("200ok");
                });
    }

    //단일 조희
    public Mono<UserCharacterResponse> updateSingleCharacter(Long id, HttpServletRequest request) {
        String userEmail= jwtTokenProvider.fetchUserEmailByHttpRequest(request);
        if ( redisService.onCoolTimeToSingleRefreshOfCharacter(id,userEmail) )
            throw new ForbiddenException(ErrorCode.ALREADY_EXISTS.getMessage(),ErrorCode.ALREADY_EXISTS);

        //먼저 디비로 가서 ocid가 있는지 확인
        QUserCharEntity qUserCharEntity = QUserCharEntity.userCharEntity;
        QUserEntity qUserEntity = QUserEntity.userEntity;

        UserCharEntity userCharEntity = jpaQueryFactory
            .selectFrom(qUserCharEntity)
            .innerJoin(qUserCharEntity.userEntity, qUserEntity)
            .where(qUserCharEntity.id.eq(id))
                .fetchOne();


        if ( !userCharEntity.getUserEntity().getUserEmail().equals(userEmail) )
            throw new ForbiddenException(ErrorCode.FORBIDDEN_EXCEPTION.getMessage(), ErrorCode.FORBIDDEN_EXCEPTION);

        //ocid 있는지 판별
        String API;
        if (userCharEntity.getOcid() == null )
            API = "/character/single?name="+userCharEntity.getCharName()+"&key="+apiKey;
        else
            API = "/character/single?ocid="+userCharEntity.getOcid()+"&key="+apiKey;

        return this.APIClient.get()
                .uri(API)
                .retrieve()
                .bodyToMono(CharacterBasic.class)
                .flatMap(result -> this.saveInExtraThread(userCharEntity, result));

    }

    //다중 조회
    @Transactional
    public Mono<List<UserCharacterResponse>> updateMultiCharacter(CharRefreshRequestDto charRefreshRequestDto, HttpServletRequest request) {
        String userEmail= jwtTokenProvider.fetchUserEmailByHttpRequest(request);
        if ( redisService.onCoolTimeToEntireRefreshOfCharacters(userEmail) )
            throw new ForbiddenException(ErrorCode.ALREADY_EXISTS.getMessage(),ErrorCode.ALREADY_EXISTS);

        //먼저 디비로 가서 ocid가 있는지 확인
        QUserCharEntity qUserCharEntity = QUserCharEntity.userCharEntity;
        QUserEntity qUserEntity = QUserEntity.userEntity;

        List<UserCharEntity> userCharEntityList = jpaQueryFactory
                .selectFrom(qUserCharEntity)
                .innerJoin(qUserCharEntity.userEntity, qUserEntity)
                .where(qUserCharEntity.id.in(charRefreshRequestDto.getIdList()))
                .fetchJoin()
                .fetch();


        ApiServerRequestDto apiServerRequestDto = new ApiServerRequestDto();
        for (UserCharEntity u : userCharEntityList){
            if ( !u.getUserEntity().getUserEmail().equals(userEmail) )
                throw new ForbiddenException(ErrorCode.FORBIDDEN_EXCEPTION.getMessage(), ErrorCode.FORBIDDEN_EXCEPTION);

            else if (u.getOcid() == null)
                apiServerRequestDto.getNameList().add(u.getCharName());

            else
                apiServerRequestDto.getOcidList().add(u.getOcid());
        }
        return this.APIClient.post()
                .uri("/character/multiple?key="+apiKey)
                .body(BodyInserters.fromValue(apiServerRequestDto))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<CharacterBasic>>() {})
                .flatMap(result -> this.saveListInExtraThread(userCharEntityList, result));

    }
    @Transactional
    public Mono<UserCharacterResponse> saveInExtraThread(UserCharEntity userCharEntity, CharacterBasic characterBasic) {
        userCharEntity.update(characterBasic);
        userCharRepository.save(userCharEntity);
        return Mono.just(new UserCharacterResponse(userCharEntity));
    }
    @Transactional
    public Mono<List<UserCharacterResponse>> saveListInExtraThread(List<UserCharEntity> userCharEntityList, List<CharacterBasic> characterBasicList) {
        List<UserCharacterResponse> resultList = new ArrayList<>();

        for (UserCharEntity u : userCharEntityList) {
            for (CharacterBasic c : characterBasicList) {
                if (u.getCharName().equals(c.getCharacter_name())){
                    resultList.add(new UserCharacterResponse(u));
                    u.update(c);
                    break;
                }
            }
        }
        userCharRepository.saveAll(userCharEntityList);
        return Mono.just(resultList);
    }



    //============내 활동관련 =======================//

    public List<BoardListResponseDto> getMyBoardList(HttpServletRequest request) {
        String userEmail = jwtTokenProvider.fetchUserEmailByHttpRequest(request);

        QBoardEntity qBoardEntity= QBoardEntity.boardEntity;

        List<BoardEntity> boardEntityList = jpaQueryFactory
                .selectFrom(qBoardEntity)
                .where(qBoardEntity.userEntity.userEmail.eq(userEmail))
                .orderBy(qBoardEntity.id.desc())
                .fetch();

        return boardEntityList.stream().map(BoardListResponseDto::new).collect(Collectors.toList());
    }

    public List<BoardListResponseDto> getMyBoardsWithCommentList(HttpServletRequest request) {
        String userEmail = jwtTokenProvider.fetchUserEmailByHttpRequest(request);

        QCommentEntity qCommentEntity = QCommentEntity.commentEntity;

        List<BoardEntity> boardEntityList = jpaQueryFactory
                .select(qCommentEntity.boardEntity)
                .distinct()
                .from(qCommentEntity)
                .where(qCommentEntity.userEmail.eq(userEmail))
                .orderBy(qCommentEntity.boardEntity.id.desc())
                .fetch();

        return boardEntityList.stream().map(BoardListResponseDto::new).collect(Collectors.toList());
    }

    public List<BoardListResponseDto> searchBoardByName(String name) {
        QBoardEntity qBoardEntity= QBoardEntity.boardEntity;

        List<BoardEntity> boardEntityList = jpaQueryFactory
                .selectFrom(qBoardEntity)
                .where(qBoardEntity.userName.eq(name))
                .orderBy(qBoardEntity.id.desc())
                .fetch();

        return boardEntityList.stream().map(BoardListResponseDto::new).collect(Collectors.toList());
    }

}
