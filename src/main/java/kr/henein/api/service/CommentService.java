package kr.henein.api.service;

import kr.henein.api.dto.comment.*;
import kr.henein.api.entity.*;
import kr.henein.api.enumCustom.UserRole;
import kr.henein.api.error.ErrorCode;
import kr.henein.api.error.exception.ForbiddenException;
import kr.henein.api.error.exception.NotFoundException;
import kr.henein.api.jwt.JwtTokenProvider;
import com.querydsl.jpa.impl.JPAQueryFactory;
import kr.henein.api.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor

public class CommentService {
    private final CommentRepository commentRepository;
    private final ReplyRepository replyRepository;
    private final BoardRepository boardRepository;
    private final JPAQueryFactory jpaQueryFactory;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final BoardCommentNumberingRepository boardCommentNumberingRepository;


    @Transactional
    public NumberingWithCommentResponseDto getCommentOfBoard(Long boardId, HttpServletRequest request) {

        QBoardEntity qBoardEntity = QBoardEntity.boardEntity;
        QCommentEntity qCommentEntity = QCommentEntity.commentEntity;
        QReplyEntity qReplyEntity = QReplyEntity.replyEntity;
        QBoardCommentNumberingEntity qNumberingEntity = QBoardCommentNumberingEntity.boardCommentNumberingEntity;

        BoardEntity boardEntity = jpaQueryFactory
                .selectFrom(qBoardEntity)
                .where(qBoardEntity.id.eq(boardId))
                .leftJoin(qBoardEntity.numberingEntityList, qNumberingEntity)
                .leftJoin(qBoardEntity.commentEntityList, qCommentEntity)
                .leftJoin(qCommentEntity.replies, qReplyEntity)
                .leftJoin(qCommentEntity.numberingEntity, qNumberingEntity)
                .leftJoin(qReplyEntity.numberingEntity, qNumberingEntity)
                .fetchOne();

        if (boardEntity == null)
            throw new NotFoundException(ErrorCode.NOT_FOUND_EXCEPTION.getMessage(), ErrorCode.NOT_FOUND_EXCEPTION);

        //AT검사. 있으면 검증, 아니면 패스
        UserEntity userEntity = null;
        String AT = jwtTokenProvider.resolveAccessToken(request);
        if (AT != null && jwtTokenProvider.validateToken(AT) ) {
            userEntity = userRepository.findByUserEmail(jwtTokenProvider.getUserEmailFromAccessToken(AT)).orElseThrow(()->{throw new NotFoundException(ErrorCode.NOT_FOUND_EXCEPTION.getMessage(),ErrorCode.NOT_FOUND_EXCEPTION);});
        }

        NumberingWithCommentResponseDto numberingWithCommentResponseDto = new NumberingWithCommentResponseDto();
        List<BoardCommentNumberingEntity> numberingEntityList = boardEntity.getNumberingEntityList();
        //넘버링 부터 만들기
        if (AT == null) {
            numberingWithCommentResponseDto.setWriterList(
                    numberingEntityList.stream().map(NumberingResponseDto::new).collect(Collectors.toList())
                    );
        } else {
            String userEmail = userEntity.getUserEmail();
            numberingWithCommentResponseDto.setWriterList(
                    numberingEntityList.stream()
                            .map(e -> new NumberingResponseDto(e, userEmail))
                            .collect(Collectors.toList())
            );
        }

        List<CommentResponseDto> commentResponseDtoList = new ArrayList<>();

        //이제 댓글 리스트 만들기
        List<CommentEntity> commentEntityList = boardEntity.getCommentEntityList();
        //진짜 미친 for 문이다;;; 이건 지옥이야!!!!
        for (CommentEntity c : commentEntityList) {
            CommentResponseDto commentResponseDto = new CommentResponseDto(c);
            for (int i =0; numberingEntityList.size() > i; i++) {
                if (!c.getDeleted()  && c.getNumberingEntity().getId() == numberingEntityList.get(i).getId()) {
                    commentResponseDto.setWriterId(i);
                    break;
                }
            }
            for (ReplyEntity r : c.getReplies()) {
                for (int i =0; numberingEntityList.size() > i; i++) {
                    if (r.getNumberingEntity().getId() == numberingEntityList.get(i).getId()) {
                        commentResponseDto.getReplies().add(new ReplyResponseDto(r,i));
                        break;
                    }
                }
            }
            commentResponseDtoList.add(commentResponseDto);
        }

        numberingWithCommentResponseDto.setCommentList(commentResponseDtoList);
        return numberingWithCommentResponseDto;
    }


    @Transactional
    public String addCommentOfParent(Long id, CommentRequestDto commentRequestDto, HttpServletRequest request){
        UserEntity userEntity = userRepository.findByUserEmail(jwtTokenProvider.fetchUserEmailByHttpRequest(request)).orElseThrow(()-> {throw new NotFoundException(ErrorCode.NOT_FOUND_EXCEPTION.getMessage(), ErrorCode.NOT_FOUND_EXCEPTION);});

        //board 가져오고, 넘버링된 리스트들도 다 가져오기
        QBoardEntity qBoardEntity = QBoardEntity.boardEntity;
        QBoardCommentNumberingEntity qNumberingEntity = QBoardCommentNumberingEntity.boardCommentNumberingEntity;
        BoardEntity boardEntity = jpaQueryFactory
                .selectFrom(qBoardEntity)
                .where(qBoardEntity.id.eq(id))
                .leftJoin(qBoardEntity.numberingEntityList, qNumberingEntity).fetchJoin()
                .fetchOne();

        if (boardEntity == null)
            throw new NotFoundException(ErrorCode.NOT_FOUND_EXCEPTION.getMessage(), ErrorCode.NOT_FOUND_EXCEPTION);

        List<BoardCommentNumberingEntity> numberingEntityList = boardEntity.getNumberingEntityList();

        //이 유저가 기존에 댓글을 작성해서 numbering 이 되어있나?
        BoardCommentNumberingEntity numberingEntity = null;
        for (BoardCommentNumberingEntity b : numberingEntityList) {
            if (b.getUserEmail().equals(userEntity.getUserEmail())) {
                numberingEntity = b;
                break;
            }
        }
        //이미 댓글을 작성한 유저면 새 댓글을 작성하고 넘버링entity 커넥션 갯수 관리
        if ( numberingEntity != null ) {
            CommentEntity commentEntity = CommentEntity.builder()
                    .comment(commentRequestDto.getComment())
                    .userEmail(userEntity.getUserEmail())
                    .boardEntity(boardEntity)
                    .numberingEntity(numberingEntity)
                    .deleted(false)
                    .updated(false)
                    .build();
            commentRepository.save(commentEntity);
            numberingEntity.updateConnectionCount(1);
            boardEntity.UpdateCommentNum(1);
            return "200ok";
        }
        String userName = commentRequestDto.isAnonymous() ? "ㅇㅇ" : userEntity.getUserName();

        // 처음 댓글다는 유저면 넘버링 엔티티 만들고 코멘트 엔티티 생성
        numberingEntity = BoardCommentNumberingEntity.builder()
                .boardEntity(boardEntity)
                .connectionCount(1)
                .userEmail(userEntity.getUserEmail())
                .nickName(userName)
                .userUid(userEntity.getUid())
                .role(checkUserRole(boardEntity.getUserEntity().getUserEmail(), userEntity))
                .build();

        boardCommentNumberingRepository.save(numberingEntity);

        CommentEntity commentEntity = CommentEntity.builder()
                .comment(commentRequestDto.getComment())
                .userEmail(userEntity.getUserEmail())
                .boardEntity(boardEntity)
                .numberingEntity(numberingEntity)
                .deleted(false)
                .updated(false)
                .build();

        commentRepository.save(commentEntity);

        //보드 게시판의 댓글수 업데이트
        boardEntity.UpdateCommentNum(1);
        return "200ok";
    }
    private UserRole checkUserRole(String writerEmail, UserEntity userEntity) {
        if (userEntity.getUserRole().equals(UserRole.ADMIN)) {
           return UserRole.ADMIN;
        } else if (writerEmail.equals(userEntity.getUserEmail())) {
            return UserRole.WRITER;
        }else {
            return UserRole.USER;
        }
    }

    @Transactional
    public String addCommentOfChild(Long id, Long coId, ReplyRequestDto replyRequestDto, HttpServletRequest request){
        String userEmail = jwtTokenProvider.fetchUserEmailByHttpRequest(request);

        UserEntity userEntity = userRepository.findByUserEmail(userEmail).orElseThrow(()->{throw new NotFoundException(ErrorCode.NOT_FOUND_EXCEPTION.getMessage(), ErrorCode.NOT_FOUND_EXCEPTION);});

        QBoardEntity qBoardEntity = QBoardEntity.boardEntity;
        QCommentEntity qCommentEntity = QCommentEntity.commentEntity;
        QBoardCommentNumberingEntity qNumberingEntity = QBoardCommentNumberingEntity.boardCommentNumberingEntity;
        //fetchJoin으로 연관된거 다 긁어 오기
        BoardEntity boardEntity = jpaQueryFactory
                .selectFrom(qBoardEntity)
                .where(qBoardEntity.id.eq(id))
                .leftJoin(qBoardEntity.commentEntityList, qCommentEntity)
                .leftJoin(qBoardEntity.numberingEntityList, qNumberingEntity)
                .fetchOne();

        if (boardEntity == null)
            throw new NotFoundException(ErrorCode.NOT_FOUND_EXCEPTION.getMessage(), ErrorCode.NOT_FOUND_EXCEPTION);

        //coId에 해당하는 댓글 찾기
        List<CommentEntity> commentEntityList = boardEntity.getCommentEntityList();
        CommentEntity targetComment = null;
        for ( CommentEntity c : commentEntityList) {
            if (c.getId() == coId) {
                targetComment = c;
                break;
            }
        }
        if (targetComment == null)
            throw new NotFoundException(ErrorCode.NOT_FOUND_EXCEPTION.getMessage(), ErrorCode.NOT_FOUND_EXCEPTION);

        //이 유저가 기존에 댓글을 작성해서 numbering 이 되어있나?
        List<BoardCommentNumberingEntity> numberingEntityList = boardEntity.getNumberingEntityList();
        BoardCommentNumberingEntity numberingEntity = null;
        for (BoardCommentNumberingEntity b : numberingEntityList) {
            if (b.getUserEmail().equals(userEntity.getUserEmail())) {
                numberingEntity = b;
                break;
            }
        }
        //이미 댓글을 작성한 유저면 새 댓글을 작성하고 넘버링entity 커넥션 갯수 관리
        if ( numberingEntity != null ) {
            ReplyEntity replyEntity = ReplyEntity.builder()
                    .tag(replyRequestDto.getTag())
                    .comment(replyRequestDto.getComment())
                    .userEmail(userEntity.getUserEmail())
                    .parent(targetComment)
                    .numberingEntity(numberingEntity)
                    .updated(false)
                    .build();

            replyRepository.save(replyEntity);
            numberingEntity.updateConnectionCount(1);
            boardEntity.UpdateCommentNum(1);
            return "200ok";
        }
        String userName = replyRequestDto.isAnonymous() ? "ㅇㅇ" : userEntity.getUserName();
        // 처음 댓글다는 유저면 넘버링 엔티티 만들고 리플 엔티티 생성
        numberingEntity = BoardCommentNumberingEntity.builder()
                .boardEntity(boardEntity)
                .connectionCount(1)
                .userEmail(userEntity.getUserEmail())
                .nickName(userName)
                .userUid(userEntity.getUid())
                .role(checkUserRole(boardEntity.getUserEntity().getUserEmail(),userEntity))
                .build();

        boardCommentNumberingRepository.save(numberingEntity);

        ReplyEntity replyEntity = ReplyEntity.builder()
                .tag(replyRequestDto.getTag())
                .comment(replyRequestDto.getComment())
                .userEmail(userEntity.getUserEmail())
                .parent(targetComment)
                .numberingEntity(numberingEntity)
                .updated(false)
                .build();

        replyRepository.save(replyEntity);

        //보드 게시판의 댓글수 업데이트
        boardEntity.UpdateCommentNum(1);

        return "200ok";
    }

    @Transactional
    public String updateCommentOfParent(Long id, Long coId, CommentRequestDto commentRequestDto, HttpServletRequest request){
        String userEmail = jwtTokenProvider.fetchUserEmailByHttpRequest(request);

        if (!boardRepository.existsById(id)) {
            throw new NotFoundException(ErrorCode.NOT_FOUND_EXCEPTION.getMessage(), ErrorCode.NOT_FOUND_EXCEPTION);
        }
        CommentEntity commentEntity = commentRepository.findById(coId).orElseThrow(()->{throw new NotFoundException("해당 댓글이 없습니다.",ErrorCode.NOT_FOUND_EXCEPTION);});
        if (!commentEntity.getUserEmail().equals(userEmail)) {
            throw new ForbiddenException(ErrorCode.FORBIDDEN_EXCEPTION.getMessage(), ErrorCode.FORBIDDEN_EXCEPTION);
        }
        commentEntity.update(commentRequestDto);
        return "수정 완료";
    }
    @Transactional
    public String updateCommentOfChild(Long id,Long reId,ReplyRequestDto replyRequestDto, HttpServletRequest request){
        String userEmail = jwtTokenProvider.fetchUserEmailByHttpRequest(request);
        if (!boardRepository.existsById(id)) {
            throw new NotFoundException(ErrorCode.NOT_FOUND_EXCEPTION.getMessage(), ErrorCode.NOT_FOUND_EXCEPTION);
        }
        ReplyEntity replyEntity = replyRepository.findById(reId).orElseThrow(()->{throw new NotFoundException("해당 댓글이 없습니다.",ErrorCode.NOT_FOUND_EXCEPTION);});
        if (!replyEntity.getUserEmail().equals(userEmail)){
            throw new ForbiddenException(ErrorCode.FORBIDDEN_EXCEPTION.getMessage(), ErrorCode.FORBIDDEN_EXCEPTION);
        }
        replyEntity.update(replyRequestDto);
        return "수정 완료";
    }

    @Transactional
    public String deleteCommentOfParent(Long id,Long coId, HttpServletRequest request ){
        String userEmail = jwtTokenProvider.fetchUserEmailByHttpRequest(request);

        QCommentEntity qCommentEntity = QCommentEntity.commentEntity;
        QBoardEntity qBoardEntity = QBoardEntity.boardEntity;
        QBoardCommentNumberingEntity qNumberingEntity = QBoardCommentNumberingEntity.boardCommentNumberingEntity;

        CommentEntity commentEntity = jpaQueryFactory
                .selectFrom(qCommentEntity)
                .where(qCommentEntity.id.eq(coId))
                .join(qCommentEntity.boardEntity, qBoardEntity).fetchJoin()
                .join(qCommentEntity.numberingEntity,qNumberingEntity).fetchJoin()
                .fetchOne();

        if (commentEntity == null) {
            throw new NotFoundException("해당 댓글이 없습니다.",ErrorCode.NOT_FOUND_EXCEPTION);
        }
        else if (!(commentEntity.getUserEmail().equals(userEmail))){
            throw new ForbiddenException(ErrorCode.FORBIDDEN_EXCEPTION.getMessage(), ErrorCode.FORBIDDEN_EXCEPTION);
        }

        //자식이 있으면 deleted == true
        if (!commentEntity.getReplies().isEmpty()){
            if (commentEntity.getNumberingEntity().getConnectionCount() == 1)
                boardCommentNumberingRepository.delete(commentEntity.getNumberingEntity());

            commentEntity.getNumberingEntity().updateConnectionCount(-1);
            commentEntity.getBoardEntity().UpdateCommentNum(-1);
            commentEntity.tempDelete();

            return "임시 삭제완료";
        }else{
            commentEntity.getBoardEntity().UpdateCommentNum(-1);
            commentEntity.getNumberingEntity().updateConnectionCount(-1);
            if (commentEntity.getNumberingEntity().getConnectionCount() == 0)
                boardCommentNumberingRepository.delete(commentEntity.getNumberingEntity());

            commentRepository.delete(commentEntity);
            return "삭제완료";
        }

    }

    @Transactional
    public String deleteCommentOfChild(Long id,Long reId,HttpServletRequest request){
        String userEmail = jwtTokenProvider.fetchUserEmailByHttpRequest(request);

        BoardEntity boardEntity = boardRepository.findById(id).orElseThrow(()->{throw new NotFoundException("해당 게시글이 없습니다.",ErrorCode.NOT_FOUND_EXCEPTION);});

        QCommentEntity qCommentEntity = QCommentEntity.commentEntity;
        QBoardCommentNumberingEntity qNumberingEntity = QBoardCommentNumberingEntity.boardCommentNumberingEntity;
        QReplyEntity qReplyEntity = QReplyEntity.replyEntity;

        ReplyEntity replyEntity = jpaQueryFactory
                .selectFrom(qReplyEntity)
                .where(qReplyEntity.id.eq(reId))
                .join(qReplyEntity.parent, qCommentEntity).fetchJoin()
                .join(qReplyEntity.numberingEntity,qNumberingEntity).fetchJoin()
                .fetchOne();

        if (replyEntity == null) {
            throw new NotFoundException("해당 리플이 없습니다.", ErrorCode.NOT_FOUND_EXCEPTION);
        } else if (!(replyEntity.getUserEmail().equals(userEmail))) {
            throw new ForbiddenException(ErrorCode.FORBIDDEN_EXCEPTION.getMessage(), ErrorCode.FORBIDDEN_EXCEPTION);
        }

        replyEntity.getNumberingEntity().updateConnectionCount(-1);
        if (replyEntity.getNumberingEntity().getConnectionCount() == 0) {
            boardCommentNumberingRepository.delete(replyEntity.getNumberingEntity());
        }

        if (replyEntity.getParent().getDeleted() && replyEntity.getParent().getReplies().size() == 1)
            commentRepository.delete(replyEntity.getParent());

        replyRepository.delete(replyEntity);

        boardEntity.UpdateCommentNum(-1);
        return "삭제완료";
    }
}
