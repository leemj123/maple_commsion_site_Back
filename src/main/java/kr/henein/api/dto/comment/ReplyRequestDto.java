package kr.henein.api.dto.comment;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;

@Getter
public class ReplyRequestDto {
    @ApiModelProperty(value="태그", example = "대댓글에 대한 태그남길때 사용", required = true)
    private String tag;
    @ApiModelProperty(value="댓글 내용", example = "문자열", required = true)
    private String comment;
    private boolean isAnonymous;
}
