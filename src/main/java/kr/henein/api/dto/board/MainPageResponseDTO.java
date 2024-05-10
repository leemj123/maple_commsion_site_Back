package kr.henein.api.dto.board;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.henein.api.enumCustom.BoardType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Schema(description = "게시판 정보")
@Builder
@Getter
@Setter
public class MainPageResponseDTO {
    @Schema(description = "관리자 지정 이름")
    private String name;
    @Schema(description = "게시판 타입",example = "Board or Layer")
    private BoardType boardType;
    @Schema(description = "관리자가 정의한 순서")
    private int numbering;
    @Schema(description = "게시글 8개 정렬")
    private List<SimpleBoardResponseDTO> simpleBoardList;
}
