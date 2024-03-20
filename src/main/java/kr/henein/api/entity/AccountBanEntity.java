package kr.henein.api.entity;

import lombok.Getter;

import javax.persistence.*;
import java.time.LocalDate;


@Entity
@Getter
public class AccountBanEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "account")
    private UserEntity userEntity;

    private LocalDate finPeriod;

    private String reason;

}
