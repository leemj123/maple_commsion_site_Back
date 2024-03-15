package kr.henein.api.dto.board;

import kr.henein.api.entity.BoardEntity;
import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;


@Getter
@AllArgsConstructor
public class BoardListResponseDto {
    private Long id;
    @ApiModelProperty(value="게시글 제목", example = "테스트 제목입니다.", required = true)
    private String title;
    @ApiModelProperty(value = "게시판 분류",example = "게시판 분류")
    private String boardType;
    @ApiModelProperty(value="댓글 갯수", example = "정수값", required = true)
    private int commentNum;
    @ApiModelProperty(value="게시글 작성자", example = "테스트 글쓴이", required = true)
    private String userName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSS")
    private LocalDateTime createTime;
    @ApiModelProperty(value="조회수", example = "정수값", required = true)
    private int views;
    @ApiModelProperty(value="좋아요수", example = "정수값", required = true)
    private int recommendNum;


    public BoardListResponseDto(BoardEntity boardEntity){
        this.id = boardEntity.getId();
        this.title = boardEntity.getTitle();
        this.boardType = boardEntity.getBoardType().getTitle();
        this.commentNum = boardEntity.getCommentNum();
        this.userName = boardEntity.getUserName();
        this.createTime = boardEntity.getCreatedDate();
        this.views = boardEntity.getViews();
        this.recommendNum = boardEntity.getRecommend();
    }
}