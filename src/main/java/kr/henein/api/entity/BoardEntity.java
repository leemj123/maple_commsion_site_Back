package kr.henein.api.entity;

import kr.henein.api.dto.board.BoardRecommendDTO;
import kr.henein.api.dto.board.BoardRequestDto;
import kr.henein.api.dto.board.TestDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Table(name= "board")
public class BoardEntity extends BaseTimeEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "type", nullable = false)
    private BoardTypeEntity type;
    @Column(nullable = false)
    private String title;
    @Column(nullable = false)
    private String userName;
    @Column
    private int commentNum;
    @ManyToOne
    @JoinColumn(name ="user", nullable = false)
    private UserEntity userEntity;
    @Column
    private int views;
    @Column
    private int recommend;
    @Column(length = 1500, nullable = false)
    private String text;
    @OneToMany(mappedBy = "boardEntity",orphanRemoval = true)
    private List<BoardCommentNumberingEntity> numberingEntityList;
    @OneToMany(mappedBy = "boardEntity", orphanRemoval = true)
    private List<CommentEntity> commentEntityList;

    @Column
    private boolean hasImage;

    @Builder
    public BoardEntity (BoardRequestDto boardRequestDto, BoardTypeEntity typeEntity, UserEntity userEntity){
        this.title = boardRequestDto.getTitle();
        if (userEntity.isAnonymous())
            this.userName = "ㅇㅇ";
        else
            this.userName = userEntity.getUserName();
        this.userEntity = userEntity;
        this.text = boardRequestDto.getText();
        this.type = typeEntity;
    }

    public void setHasImage(boolean value) {
        this.hasImage = value;
    }

    public void Update(TestDto testDto){
        this.title = testDto.getTitle();
        this.text = testDto.getText();
    }
    public void Update(BoardRecommendDTO boardRecommendDTO){
        this.recommend = boardRecommendDTO.getRecommend();
    }
    public void UpdateCommentNum(int num){
        this.commentNum += num;
    }
    public void UpdateView(){
        this.views += 1;
    }
}
