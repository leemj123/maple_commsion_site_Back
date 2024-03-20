package kr.henein.api.entity;

import kr.henein.api.enumCustom.BoardType;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
@Table(name= "type")
public class BoardTypeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private BoardType boardType;

    @Column(nullable = false, unique = true)
    private int numbering;

    @OneToMany(mappedBy = "type")
    private List<BoardEntity> boardEntityList;

    public BoardTypeEntity (String name, BoardType boardType, int numbering) {
        this.name = name;
        this.boardType = boardType;
        this.numbering = numbering;
        this.boardEntityList = new ArrayList<>();
    }
}
