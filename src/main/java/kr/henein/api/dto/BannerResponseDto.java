package kr.henein.api.dto;


import kr.henein.api.entity.BannerEntity;

public class BannerResponseDto {

    private final Long id;
    private final String text;
    private final String url;
    private final int numbering;

    public BannerResponseDto(BannerEntity bannerEntity) {
        this.id = bannerEntity.getId();
        this.text = bannerEntity.getText();
        this.url = bannerEntity.getUrl();
        this.numbering = bannerEntity.getNumbering();
    }
}
