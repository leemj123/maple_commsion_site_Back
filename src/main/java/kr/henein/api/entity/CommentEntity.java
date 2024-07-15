package kr.henein.api.entity;

import kr.henein.api.dto.comment.CommentRequestDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Entity
@Builder
public class CommentEntity extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String comment;
    @Column(nullable = false)
    private String userEmail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false)
    private BoardEntity boardEntity;

    @ManyToOne
    @JoinColumn(name = "numbering_id")
    private BoardCommentNumberingEntity numberingEntity;

    @OneToMany(mappedBy = "parent",orphanRemoval = true)
    private List<ReplyEntity> replies;
    @Column(nullable = false)
    private Boolean deleted;
    @Column(nullable = false)
    private Boolean updated;
    @Column(nullable = false)
    private boolean isAnonymous;

    public void update(CommentRequestDto commentRequestDto) {
        this.comment = commentRequestDto.getComment();
        this.updated = true;
    }
    public void tempDelete(){
        this.numberingEntity = null;
        this.deleted = true;
        this.comment = "삭제된 댓글입니다.";
    }
}
