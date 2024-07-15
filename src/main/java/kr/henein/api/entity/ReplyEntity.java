package kr.henein.api.entity;

import kr.henein.api.dto.comment.ReplyRequestDto;
import lombok.*;

import javax.persistence.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Entity
@Builder
public class ReplyEntity extends BaseTimeEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column
    private String tag;
    @Column(nullable = false)
    private String comment;
    @Column(nullable = false)
    private String userEmail;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "parent_id",nullable = false)
    private CommentEntity parent;

    @ManyToOne
    @JoinColumn(name = "numbering_id",nullable = false)
    private BoardCommentNumberingEntity numberingEntity;

    @Column(nullable = false)
    private Boolean updated;

    private boolean isAnonymous;
    public void update(ReplyRequestDto replyRequestDto) {
        this.tag = replyRequestDto.getTag();
        this.comment = replyRequestDto.getComment();
        this.updated = true;
    }
}
