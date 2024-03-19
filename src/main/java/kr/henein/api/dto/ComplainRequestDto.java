package kr.henein.api.dto;

import io.swagger.annotations.ApiModelProperty;
import kr.henein.api.enumCustom.ComplainReason;
import kr.henein.api.enumCustom.ComplainType;
import lombok.Getter;

@Getter
public class ComplainRequestDto {
    @ApiModelProperty(value="신고할 데이터 타입", example = "Board or Comment or Reply", required = true)
    private ComplainType complainType;
    private ComplainReason complainReason;
    private Long targetId;
    private String text;
}
