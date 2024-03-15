package kr.henein.api.dto.board;


import kr.henein.api.entity.BoardEntity;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Objects;

@Getter
@AllArgsConstructor
public class BoardResponseDto {
    private Long id;
    @ApiModelProperty(value="게시글 타입", example = "Advertise", required = true)
    private String boardType;
    @ApiModelProperty(value="게시글 제목", example = "테스트 제목입니다.", required = true)
    private String title;
    @ApiModelProperty(value="작성자", example = "작성자", required = true)
    private String userName;
    @ApiModelProperty(value="본인 글 식별", example = "작성자 고유Id", required = true)
    private String uid;
    @ApiModelProperty(value="댓글 갯수", example = "정수값", required = true)
    private int commentNum;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSS")
    private LocalDateTime createTime;
    @ApiModelProperty(value="조회수", example = "정수값", required = true)
    private int views;
    @ApiModelProperty(value="추천받은 수", example = "정수값", required = true)
    private int recommend;
    @ApiModelProperty(value="게시글 내용", example = "테스트 내용입니다~~", required = true)
    private String text;
    @ApiModelProperty(value="사진 첨부 여부",example = "T or F",required = true)
    private boolean hasImage;
    @ApiModelProperty(value = "추천했는지", example = "T or F")
    private boolean recommended;

    public BoardResponseDto (BoardEntity boardEntity, boolean recommended, String uid){
        this.id = boardEntity.getId();
        this.boardType =boardEntity.getType().getType();
        this.title = boardEntity.getHtmlTitle();
        this.commentNum = boardEntity.getCommentNum();
        this.userName = boardEntity.getUserName();
        if (Objects.equals(uid,boardEntity.getUserEntity().getUid())) {
            this.uid = boardEntity.getUserEntity().getUid();
        }
        this.createTime = boardEntity.getCreatedDate();
        this.views = boardEntity.getViews();
        this.hasImage = boardEntity.isHasImage();
        this.recommend = boardEntity.getRecommend();
        this.text = boardEntity.getHtmlText();
        this.recommended = recommended;
    }
}