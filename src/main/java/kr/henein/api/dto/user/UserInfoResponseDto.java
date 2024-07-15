package kr.henein.api.dto.user;

import kr.henein.api.entity.UserEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserInfoResponseDto {
    private String userName;
    private String userRole;
    private String pickCharacter;
    private String imageUrl;

    public UserInfoResponseDto(UserEntity userEntity, String pickCharacter, String imageUrl) {
        this.userName = userEntity.getUserName();
        this.userRole = userEntity.getUserRole().toString();
        this.pickCharacter = pickCharacter;
        this.imageUrl = imageUrl;
    }
}
