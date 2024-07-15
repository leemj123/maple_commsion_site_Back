package kr.henein.api.dto.user;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class UserDetailInfoResponseDto {
    private String userName;
    private String uid;
    private String imageUrl;
    private LocalDate signUpDate;
    private long boardCount;
    private long commentCount;


}
