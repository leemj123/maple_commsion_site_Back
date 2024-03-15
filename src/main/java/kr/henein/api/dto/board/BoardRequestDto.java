package kr.henein.api.dto.board;


import lombok.Getter;
import lombok.Setter;


import javax.persistence.Lob;


@Getter
@Setter
public class BoardRequestDto {
    private String htmlTitle;
    private String title;
    @Lob
    private String htmlText;
    @Lob
    private String text;
    private String boardType;

}
