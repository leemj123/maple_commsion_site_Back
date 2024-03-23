package kr.henein.api.entity;

import lombok.Getter;

import javax.persistence.*;

@Entity
@Getter
public class BannerEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String text;

    @Column
    private String url;

    @Column(nullable = false, unique = true)
    private int numbering;
}
