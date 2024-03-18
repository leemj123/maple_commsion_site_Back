package kr.henein.api.dto.board;

import kr.henein.api.enumCustom.BoardType;
import lombok.Builder;

import java.util.List;

@Builder
public class MainPageResponseDTO {

    private String name;

    private BoardType boardType;

    private int numbering;

    private List<SimpleBoardResponseDTO> simpleBoardList;
}
