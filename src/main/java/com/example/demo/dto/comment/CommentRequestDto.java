package com.example.demo.dto.comment;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CommentRequestDto {
    //@ApiModelProperty(value="댓글 내용", example = "문자열", required = true)
    private String comment;

}
