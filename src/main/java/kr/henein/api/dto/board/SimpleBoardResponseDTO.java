package kr.henein.api.dto.board;


import kr.henein.api.entity.BoardEntity;
import lombok.Getter;

@Getter
public class SimpleBoardResponseDTO {
    private final Long id;
    private final String title;
    private final String userName;

    public SimpleBoardResponseDTO (BoardEntity boardEntity) {
        this.id = boardEntity.getId();
        this.title = boardEntity.getTitle();
        this.userName = boardEntity.getUserName();
    }
}
