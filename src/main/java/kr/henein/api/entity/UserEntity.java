package kr.henein.api.entity;

import kr.henein.api.enumCustom.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


import javax.persistence.*;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class UserEntity extends BaseTimeEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String uid;
    @Column(nullable = false)
    private UserRole userRole;
    @Column(unique = true, nullable = false)
    private String userEmail;
    @Column(unique = true, nullable = false)
    private String userName;
    @Column
    private String password;
    @Column(length = 512,nullable = false)
    private String refreshToken;
    @Column
    private String nexonApiKey;

    @OneToOne
    @JoinColumn(name = "ban")
    private AccountBanEntity accountBanEntity;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "pick_char")
    private UserCharEntity pickChar;


    //카카오 로그인을 위함
    public UserEntity(String email, int randomNum) {
        String uid = UUID.randomUUID().toString();
        this.userEmail = email;
        this.userName = "ㅇㅇ"+randomNum;
        this.userRole = UserRole.USER;
        this.uid = String.valueOf(uid);
    }

    public void updatePickChar(UserCharEntity charEntity) {this.pickChar = charEntity;}
    public void updateUserName(String userName) {
        this.userName = userName;
    }

    public void setRefreshToken(String refreshToken){
        this.refreshToken = refreshToken;
    }
    public void UpdateApiKey(String nexonApiKey) {
        this.nexonApiKey = nexonApiKey;
    }
}
