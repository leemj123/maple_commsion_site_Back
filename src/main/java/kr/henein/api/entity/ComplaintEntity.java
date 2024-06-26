package kr.henein.api.entity;

import kr.henein.api.enumCustom.ComplainReason;
import kr.henein.api.enumCustom.ComplainType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ComplaintEntity extends BaseTimeEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private ComplainType complainType;

    @Column(nullable = false)
    private ComplainReason complainReason;

    @Column(nullable = false)
    private Long targetId;

    @Column(nullable = false)
    private String text;

    @Column(nullable = false)
    private Long complainerId;

    @Column(nullable = false)
    private String complainerName;

    @Column(nullable = false)
    private Long complainantId;

    @Column(nullable = false)
    private String complainantName;

}
