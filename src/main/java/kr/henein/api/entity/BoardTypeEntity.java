package kr.henein.api.entity;

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
    private String type;

    @OneToMany(mappedBy = "type", orphanRemoval = false)
    private List<BoardEntity> boardEntityList;

    public BoardTypeEntity (String type) {
        this.type = type;
        boardEntityList = new ArrayList<>();
    }

}
