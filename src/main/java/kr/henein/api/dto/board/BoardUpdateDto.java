package kr.henein.api.dto.board;

import lombok.Getter;
import org.springframework.stereotype.Service;

import javax.persistence.Lob;

@Getter
@Service
public class BoardUpdateDto {

    private String title;
    @Lob
    private String htmlText;
    @Lob
    private String text;


}
