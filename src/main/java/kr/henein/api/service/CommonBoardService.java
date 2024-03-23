package kr.henein.api.service;

import com.querydsl.jpa.impl.JPAQueryFactory;
import io.jsonwebtoken.Claims;
import kr.henein.api.dto.BannerResponseDto;
import kr.henein.api.dto.board.*;
import kr.henein.api.entity.*;

import kr.henein.api.enumCustom.BoardType;
import kr.henein.api.enumCustom.S3EntityType;
import kr.henein.api.enumCustom.UserRole;
import kr.henein.api.error.ErrorCode;
import kr.henein.api.error.exception.ForbiddenException;
import kr.henein.api.error.exception.NotFoundException;
import kr.henein.api.jwt.JwtTokenProvider;
import kr.henein.api.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommonBoardService {
    private final BoardRepository boardRepository;
    private final RecommendRepository recommendRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final S3FileRepository s3FileRepository;
    private final JPAQueryFactory jpaQueryFactory;
    private final S3Service s3Service;
    private final BoardTypeRepository boardTypeRepository;
    private final BannerRepository bannerRepository;

    private BoardTypeEntity getBoardType(String type) {
        return boardTypeRepository.findByName(type).orElseThrow(()->new NotFoundException(ErrorCode.NOT_EXIST_TYPE.getMessage(), ErrorCode.NOT_EXIST_TYPE));
    }
    public String[] getTypeList() {

        List<BoardTypeEntity> boardTypeEntityList = boardTypeRepository.findAllByBoardType(BoardType.Board);

        return boardTypeEntityList.stream()
                .map(BoardTypeEntity::getName)
                .toArray(String[]::new);
    }

    public Page<BoardListResponseDto> getTypeOfBoard(int page, String type){

        PageRequest pageRequest = PageRequest.of(page-1, 20);
        BoardTypeEntity boardTypeEntity = boardTypeRepository.findByName(type).orElseThrow(()->new NotFoundException(ErrorCode.NOT_EXIST_TYPE.getMessage(), ErrorCode.NOT_EXIST_TYPE));
        QBoardTypeEntity qBoardTypeEntity = QBoardTypeEntity.boardTypeEntity;
        QBoardEntity qBoardEntity = QBoardEntity.boardEntity;

        List<BoardEntity> resultEntityList = jpaQueryFactory
                .selectFrom(qBoardEntity)
                .innerJoin(qBoardEntity.type, qBoardTypeEntity)
                .where(qBoardEntity.type.eq(boardTypeEntity))
                .orderBy(qBoardEntity.id.desc())
                .offset(pageRequest.getOffset())
                .limit(20)
                .fetch();

        long count = jpaQueryFactory
                .selectFrom(qBoardEntity)
                .join(qBoardEntity.type, qBoardTypeEntity)
                .where(qBoardEntity.type.eq(boardTypeEntity))
                .fetchCount();

        return new PageImpl<>(resultEntityList.stream().map(BoardListResponseDto::new).collect(Collectors.toList()), pageRequest,count);

    }
    public Page<BoardListResponseDto> getBoardNotNotice(int page) {
        PageRequest pageRequest = PageRequest.of(page-1, 20);
        BoardTypeEntity type = boardTypeRepository.findByName("공지").orElseThrow(()->new NotFoundException("공지 게시판이 왜 없지 이상하네 ㅎㅎ..",ErrorCode.NOT_EXIST_TYPE));

        QBoardTypeEntity qBoardTypeEntity = QBoardTypeEntity.boardTypeEntity;
        QBoardEntity qBoardEntity = QBoardEntity.boardEntity;

        List<BoardEntity> resultEntityList = jpaQueryFactory
                .selectFrom(qBoardEntity)
                .leftJoin(qBoardEntity.type, qBoardTypeEntity)
                .where(qBoardEntity.type.ne(type))
                .orderBy(qBoardEntity.id.desc())
                .offset(pageRequest.getOffset())
                .limit(pageRequest.getPageSize())
                .fetch();

        long count = jpaQueryFactory
                .selectFrom(qBoardEntity)
                .where(qBoardEntity.type.ne(type))
                .fetchCount();

        return new PageImpl<>(resultEntityList.stream().map(BoardListResponseDto::new).collect(Collectors.toList()), pageRequest, count);
    }
    public Page<BoardListResponseDto> SearchByText(String type, String key, int page) {
        PageRequest pageRequest = PageRequest.of(page-1, 20);

        if ( type.equals("ALL") ) {
            Page<BoardEntity> result = boardRepository.searchByText(key,pageRequest);

            return result.map(BoardListResponseDto::new);
        } else {
            BoardTypeEntity boardType = getBoardType(type);
            Page<BoardEntity> result = boardRepository.searchByTextWithType(key, boardType, pageRequest);

            return result.map(BoardListResponseDto::new);
        }
    }

    public List<MainPageResponseDTO> getMainPageService() {
        List<MainPageResponseDTO> resultList = new ArrayList<>();

        QBoardEntity qBoardEntity = QBoardEntity.boardEntity;
        QBoardTypeEntity qBoardTypeEntity = QBoardTypeEntity.boardTypeEntity;
        {
            List<BoardEntity> AllType = jpaQueryFactory
                    .selectFrom(qBoardEntity)
                    .orderBy(qBoardEntity.id.desc())
                    .limit(8)
                    .fetch();

            MainPageResponseDTO AllTypeDto = MainPageResponseDTO.builder()
                    .name("ALL")
                    .boardType(BoardType.Board)
                    .numbering(-1)
                    .simpleBoardList(AllType.stream().map(SimpleBoardResponseDTO::new).collect(Collectors.toList()))
                    .build();

            resultList.add(AllTypeDto);
        }

        List<BoardTypeEntity> boardTypeEntityList = boardTypeRepository.findAllByOrderByNumberingAsc();

        for (BoardTypeEntity b: boardTypeEntityList) {
            if (b.getBoardType().equals(BoardType.Layer)) {
                MainPageResponseDTO layerDto = MainPageResponseDTO.builder()
                        .boardType(BoardType.Layer)
                        .numbering(b.getNumbering())
                        .build();

                resultList.add(layerDto);
            }
            List<BoardEntity> typeByBoard = jpaQueryFactory
                    .selectFrom(qBoardEntity)
                    .innerJoin(qBoardEntity.type, qBoardTypeEntity)
                    .where(qBoardEntity.type.eq(b))
                    .orderBy(qBoardEntity.id.desc())
                    .limit(8)
                    .fetch();

            MainPageResponseDTO typeDto = MainPageResponseDTO.builder()
                    .name(b.getName())
                    .boardType(BoardType.Board)
                    .numbering(-1)
                    .simpleBoardList(typeByBoard.stream().map(SimpleBoardResponseDTO::new).collect(Collectors.toList()))
                    .build();

            resultList.add(typeDto);
        }


        return resultList;
    }


    //===================================================================================================
    @Transactional
    public long addTypeOfBoard(BoardRequestDto boardRequestDto, HttpServletRequest request){
        Claims claims = jwtTokenProvider.getClaimsByRequest(request);
        String userEmail = claims.getSubject();
        UserRole userRole = UserRole.valueOf((String) claims.get("ROLE"));

        BoardTypeEntity boardType = getBoardType(boardRequestDto.getBoardType());

        if (boardType.getName().equals("NOTICE") && !userRole.equals(UserRole.ADMIN)) {
            throw new ForbiddenException(ErrorCode.FORBIDDEN_EXCEPTION.getMessage(), ErrorCode.FORBIDDEN_EXCEPTION);
        }

        UserEntity userEntity = userRepository.findByUserEmail(userEmail).orElseThrow(()-> new NotFoundException(ErrorCode.NOT_FOUND_EXCEPTION.getMessage(), ErrorCode.NOT_FOUND_EXCEPTION));

        BoardEntity boardEntity = new BoardEntity(boardRequestDto,boardType, userEntity);
        BoardEntity savedBoard = boardRepository.save(boardEntity);

        //이미지 파일 첨부되어있는지 문자열 슬라이싱
        String regex = "(https://henesys-bucket.s3.ap-northeast-2.amazonaws.com/.*?\\.jpg)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(boardRequestDto.getText());

        List<String> imagesUrl = new ArrayList<>();
        while (matcher.find()){
            imagesUrl.add(matcher.group(1));
        }
        //이미지가 있으면 해당 이미지를 사용중인거로 업데이트
        if (!imagesUrl.isEmpty()){
            savedBoard.setHasImage(true);
            s3Service.changeImageInfo(imagesUrl, S3EntityType.BOARD, savedBoard.getId());
        }

        return savedBoard.getId();
    }
    //=============================================================================
    public BoardResponseDto getOneService(Long id, String authentication){

        BoardEntity boardEntity = boardRepository.findById(id).orElseThrow(()-> new NotFoundException("해당 게시글 정보가 없습니다", ErrorCode.NOT_FOUND_EXCEPTION));

        if (authentication != null){ //사용자가 이 게시판에 대해서 추천했는지에 대한 t f 적용
            authentication = authentication.substring(7);
            jwtTokenProvider.validateToken(authentication);

            String userEmail = jwtTokenProvider.getUserEmailFromAccessToken(authentication); // 정보 가져옴
            UserEntity userEntity = userRepository.findByUserEmail(userEmail).orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + userEmail));

            boardEntity.UpdateView();

            RecommendEntity recommend = recommendRepository.findByBoardEntityAndUserEntity(boardEntity, userEntity);
            if ( recommend == null){
                BoardResponseDto boardResponseDto = new BoardResponseDto(boardEntity,false, userEntity.getUid());
                return boardResponseDto;
            }
            BoardResponseDto boardResponseDto = new BoardResponseDto(boardEntity,recommend.isValue(), userEntity.getUid());
            return boardResponseDto;
        }
        BoardResponseDto boardResponseDto = new BoardResponseDto(boardEntity,false,null);
        return boardResponseDto;
    }


    @Transactional
    public long updateService(Long id, BoardUpdateDto boardUpdateDto, HttpServletRequest request){
        String userEmail = jwtTokenProvider.fetchUserEmailByHttpRequest(request);
        BoardEntity boardEntity = boardRepository.findById(id).orElseThrow(()-> new NotFoundException("해당 게시글 정보가 없습니다", ErrorCode.NOT_FOUND_EXCEPTION));
        if (!boardEntity.getUserEntity().getUserEmail().equals(userEmail)){
            throw new ForbiddenException("게시글 수정 권한이 없습니다.",ErrorCode.FORBIDDEN_EXCEPTION);
        }

        //이미지 파일 첨부되어있는지 문자열 슬라이싱
        String regex = "(https://henesys-bucket.s3.ap-northeast-2.amazonaws.com/.*?\\.jpg)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(boardUpdateDto.getText());

        List<String> imagesUrl = new ArrayList<>();
        while (matcher.find()){
            imagesUrl.add(matcher.group(1));
        }
        //이미지가 없으면 연결되어 있던 사진entity들 non_use로 처리
        if(imagesUrl.isEmpty() && boardEntity.isHasImage()){
            List<S3File> fileList = s3FileRepository.findAllByS3EntityTypeAndTypeId(S3EntityType.BOARD,id);
            fileList.stream().forEach(s3File -> s3File.setEntityData(S3EntityType.NON_USED,null));
            boardEntity.setHasImage(false);
        }
        //이미지가 있으면 해당 이미지를 사용중인거로 업데이트
        //1. 현재 게시판에 맵핑되어있는 엔티티들 긁어오고
        //2. url로 매칭되는 엔티티 다 긁어오고
        //3. id로 게시글에 맵핑된거 다 긁어오고
        //4. 행동 수행
        else if (imagesUrl != null){
            boardEntity.setHasImage(true);
            List<S3File> savedList = s3FileRepository.findAllByS3EntityTypeAndTypeId(S3EntityType.BOARD,id);
            List<S3File> findList = new ArrayList<>();
            for (int i = 0; i < imagesUrl.size();i++){
                findList.add(s3FileRepository.findByFileUrl(imagesUrl.get(i)));
            }
            // 이런 비교를 위해서는 Set이 좋다
            Set<S3File> savedSet = new HashSet<>(savedList);
            Set<S3File> findSet = new HashSet<>(findList);
            // 저장해야될것 리스트
            Set<S3File> toBeAdded = new HashSet<>(findSet);
            toBeAdded.removeAll(savedSet);
            // 삭제해야될 리스트
            Set<S3File> toBeRemoved = new HashSet<>(savedSet);
            toBeRemoved.removeAll(findSet);
            //행동 수행
            toBeAdded.stream().forEach(s3File -> s3File.setEntityData(S3EntityType.BOARD,id));
            toBeRemoved.stream().forEach(s3File -> s3File.setEntityData(S3EntityType.NON_USED,id));
        }

        boardEntity.Update(boardUpdateDto);
        return boardEntity.getId();
    }
    @Transactional
    public String deleteService(Long id, HttpServletRequest request ){
        String userEmail = jwtTokenProvider.fetchUserEmailByHttpRequest(request);
        BoardEntity boardEntity = boardRepository.findById(id).orElseThrow(()-> new NotFoundException("해당 게시글 정보가 없습니다", ErrorCode.NOT_FOUND_EXCEPTION));
        if (!boardEntity.getUserEntity().getUserEmail().equals(userEmail)){
            throw new ForbiddenException("게시글 수정 권한이 없습니다.",ErrorCode.FORBIDDEN_EXCEPTION);
        }
        //게시글에 저장되어있던 사진들 전부 미사용으로 전환
        List<S3File> fileList = s3FileRepository.findAllByS3EntityTypeAndTypeId(S3EntityType.BOARD,id);
        fileList.stream().forEach(s3File -> s3File.setEntityData(S3EntityType.NON_USED, null));
        boardRepository.delete(boardEntity);

        return "삭제완료";
    }
    @Transactional
    public String recommendThisBoard(Long id, HttpServletRequest request ){
        BoardEntity boardEntity = boardRepository.findById(id).orElseThrow(()-> new NotFoundException("해당 게시글 정보가 없습니다", ErrorCode.NOT_FOUND_EXCEPTION));
        String userEmail = jwtTokenProvider.fetchUserEmailByHttpRequest(request);
        UserEntity userEntity = userRepository.findByUserEmail(userEmail).orElseThrow(()-> new NotFoundException(ErrorCode.NOT_FOUND_EXCEPTION.getMessage(), ErrorCode.NOT_FOUND_EXCEPTION));

        RecommendEntity recommendEntity = recommendRepository.findByBoardEntityAndUserEntity(boardEntity,userEntity);
        //추천 DB에 없는 인원일때 ( 해당 게시글에 처음 추천을 누른 유저일시 )
        if (null == recommendEntity){
            RecommendEntity recommend = RecommendEntity.builder()
                    .boardEntity(boardEntity)
                    .userEntity(userEntity)
                    .value(true)
                    .build();
            BoardRecommendDTO boardRecommendDTO = new BoardRecommendDTO(boardEntity.getRecommend()+1);
            boardEntity.Update(boardRecommendDTO);

            recommendRepository.save(recommend);
            return "추천 완료";
        }
        //이미 추천한 흔적이 있는 유저들
        if (recommendEntity.isValue()){ //true ?
            BoardRecommendDTO boardRecommendDTO = new BoardRecommendDTO(boardEntity.getRecommend()-1);
            boardEntity.Update(boardRecommendDTO);
            recommendEntity.setValue(false);

            return "추천 취소";
        } else {
            BoardRecommendDTO boardRecommendDTO = new BoardRecommendDTO(boardEntity.getRecommend()+1);
            boardEntity.Update(boardRecommendDTO);
            recommendEntity.setValue(true);

            return "재추천 완료";
        }
    }

    public List<BannerResponseDto> getBannerList() {
        return bannerRepository.findAllByOrderByNumberingAsc().stream().map(BannerResponseDto::new).collect(Collectors.toList());
    }
}
