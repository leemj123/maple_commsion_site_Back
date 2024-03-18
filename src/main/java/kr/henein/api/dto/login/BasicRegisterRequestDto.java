package kr.henein.api.dto.login;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;

@Getter
public class BasicRegisterRequestDto {
    @ApiModelProperty(value="유저 고유 email", example = "~@naver.com", required = true)
    private String userEmail;
    @ApiModelProperty(value="유저 비밀번호", example = "문자열", required = true)
    private String password;

}
