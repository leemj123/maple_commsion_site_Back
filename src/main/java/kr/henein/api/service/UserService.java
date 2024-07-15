package kr.henein.api.service;

import com.querydsl.core.types.dsl.BooleanExpression;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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
        UserEntity userEntity = userRepository.findByUserEmail(userEmail).orElseThrow(()-> new NotFoundException(ErrorCode.NOT_FOUND_EXCEPTION.getMessage(), ErrorCode.NOT_FOUND_EXCEPTION));




        List<S3File> s3File = s3FileRepository.findAllByS3EntityTypeAndTypeId(S3EntityType.USER,userEntity.getId());

        if (s3File.isEmpty() && userEntity.getPickChar() == null ) {
            return new UserInfoResponseDto(userEntity,null,null);
        }
        else if (userEntity.getPickChar() == null) {
            return new UserInfoResponseDto(userEntity,null,s3File.get(0).getFileUrl());
        }
        else if (s3File.isEmpty()) {
            return new UserInfoResponseDto(userEntity,userEntity.getPickChar().getCharName(),null);
        }

        return new UserInfoResponseDto(userEntity,userEntity.getPickChar().getCharName(),s3File.get(0).getFileUrl());
    }

    public UserDetailInfoResponseDto userDetailInfo(String userName, HttpServletRequest request) {
        String requestUserEmail = jwtTokenProvider.fetchUserEmailByHttpRequest(request);
        UserEntity userEntity = userRepository.findByUserName(userName).orElseThrow(()-> new NotFoundException(ErrorCode.NOT_FOUND_EXCEPTION.getMessage(), ErrorCode.NOT_FOUND_EXCEPTION));

        QBoardEntity qBoardEntity= QBoardEntity.boardEntity;
        BooleanExpression predicate = qBoardEntity.userEntity.eq(userEntity);

        String uid = userEntity.getUid();
        if ( !requestUserEmail.equals(userEntity.getUserEmail()) ){
            predicate = predicate.and(qBoardEntity.isAnonymous.isFalse());
            uid = null;
        }


        long boardCount = jpaQueryFactory
                .selectFrom(qBoardEntity)
                .where(predicate)
                .fetchCount();

        QCommentEntity qCommentEntity = QCommentEntity.commentEntity;
        long commentCount = jpaQueryFactory
                .selectFrom(qCommentEntity)
                .where(predicate)
                .fetchCount();

        List<S3File> s3File = s3FileRepository.findAllByS3EntityTypeAndTypeId(S3EntityType.USER,userEntity.getId());

        if (s3File.isEmpty()) {
            return UserDetailInfoResponseDto.builder()
                    .userName(userEntity.getUserName())
                    .uid(uid)
                    .imageUrl(null)
                    .boardCount(boardCount)
                    .commentCount(commentCount)
                    .build();
        }
        return UserDetailInfoResponseDto.builder()
                .userName(userEntity.getUserName())
                .uid(uid)
                .imageUrl(s3File.get(0).getFileUrl())
                .boardCount(boardCount)
                .commentCount(commentCount)
                .build();
    }
    //============내 활동관련 =======================//

    public Page<BoardListResponseDto> getMyBoardList(HttpServletRequest request, String name, int page, int size) {

        String requestUserEmail = jwtTokenProvider.fetchUserEmailByHttpRequest(request);

        UserEntity targetedUser = userRepository.findByUserName(name).orElseThrow(()->new NotFoundException(ErrorCode.NOT_FOUND.getMessage(), ErrorCode.NOT_FOUND_EXCEPTION));

        QBoardEntity qBoardEntity= QBoardEntity.boardEntity;
        BooleanExpression predicate = qBoardEntity.userEntity.eq(targetedUser);

        if( !requestUserEmail.equals(targetedUser.getUserEmail()) ) {
            predicate = predicate.and(qBoardEntity.isAnonymous.isFalse());
        }


        PageRequest pageRequest = PageRequest.of(page-1,size);


        List<BoardEntity> boardEntityList = jpaQueryFactory
                .selectFrom(qBoardEntity)
                .where(predicate)
                .orderBy(qBoardEntity.id.desc())
                .offset(pageRequest.getOffset())
                .limit(pageRequest.getPageSize())
                .fetch();

        long count = jpaQueryFactory
                .selectFrom(qBoardEntity)
                .where(predicate)
                .fetchCount();

        return new PageImpl<>(boardEntityList.stream().map(BoardListResponseDto::new).collect(Collectors.toList()), pageRequest, count);
    }

    public Page<BoardListResponseDto> getMyBoardsWithCommentList(HttpServletRequest request, String name, int page, int size) {
        String requestUserEmail = jwtTokenProvider.fetchUserEmailByHttpRequest(request);

        UserEntity targetedUser = userRepository.findByUserName(name).orElseThrow(()->new NotFoundException(ErrorCode.NOT_FOUND.getMessage(), ErrorCode.NOT_FOUND_EXCEPTION));

        QCommentEntity qCommentEntity = QCommentEntity.commentEntity;

        BooleanExpression predicate = qCommentEntity.userEmail.eq(targetedUser.getUserEmail());

        if ( !requestUserEmail.equals(targetedUser.getUserEmail()) )
            predicate = predicate.and(qCommentEntity.boardEntity.userName.ne("ㅇㅇ"));


        PageRequest pageRequest = PageRequest.of(page-1,size);

        List<BoardEntity> boardEntityList = jpaQueryFactory
                .select(qCommentEntity.boardEntity)
                .distinct()
                .from(qCommentEntity)
                .where(predicate)
                .orderBy(qCommentEntity.boardEntity.id.desc())
                .offset(pageRequest.getOffset())
                .limit(pageRequest.getPageSize())
                .fetch();

        long count = jpaQueryFactory
                .select(qCommentEntity.boardEntity)
                .distinct()
                .from(qCommentEntity)
                .where(predicate)
                .fetchCount();

        return new PageImpl<>(boardEntityList.stream().map(BoardListResponseDto::new).collect(Collectors.toList()), pageRequest, count);
    }

    @Transactional
    public String userUpdate(UserInfoChange userInfoChange, HttpServletRequest request) throws IOException {
        String userEmail = jwtTokenProvider.fetchUserEmailByHttpRequest(request);
        UserEntity userEntity = userRepository.findByUserEmail(userEmail).orElseThrow(()-> new NotFoundException(ErrorCode.NOT_FOUND_EXCEPTION.getMessage(), ErrorCode.NOT_FOUND_EXCEPTION));

        if (!(userInfoChange.getImage() == null || userInfoChange.getImage().isEmpty())) {
            s3Service.uploadImageUserPicture(userInfoChange.getImage(), userEntity.getId());
        }

        return "200ok";
    }

    //=============================캐릭터 관련===================================================================================
    @Transactional
    public void pickCharacter(Long id, HttpServletRequest request) {
        String userEmail = jwtTokenProvider.fetchUserEmailByHttpRequest(request);
        UserEntity userEntity = userRepository.findByUserEmail(userEmail).orElseThrow(()-> new NotFoundException(ErrorCode.NOT_FOUND_EXCEPTION.getMessage(), ErrorCode.NOT_FOUND_EXCEPTION));


        if (userEntity.getPickChar() == null){
            UserCharEntity newCharEntity = userCharRepository.findByUserEntityAndId(userEntity, id);
            newCharEntity.pickThisCharacter();

            userEntity.updatePickChar(newCharEntity);
            return;
        }
        else if (userEntity.getPickChar().getId().equals(id) ){
            userEntity.getPickChar().unPickThisCharacter();
            userEntity.updatePickChar(null);
            return;
        }

        UserCharEntity newCharEntity = userCharRepository.findByUserEntityAndId(userEntity, id);

        userEntity.getPickChar().unPickThisCharacter();
        newCharEntity.pickThisCharacter();
        userEntity.updatePickChar(newCharEntity);

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
        UserEntity userEntity = userRepository.findByUserEmail(userEmail).orElseThrow(()-> new NotFoundException(ErrorCode.NOT_FOUND_EXCEPTION.getMessage(), ErrorCode.NOT_FOUND_EXCEPTION));

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
