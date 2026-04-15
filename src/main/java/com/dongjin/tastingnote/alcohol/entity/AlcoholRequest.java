package com.dongjin.tastingnote.alcohol.entity;

import com.dongjin.tastingnote.common.BaseEntity;
import com.dongjin.tastingnote.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "alcohol_request")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AlcoholRequest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by", nullable = false)
    private User requestedBy;

    @Column(nullable = false)
    private String name;

    @Column(name = "name_ko")
    private String nameKo;

    @ElementCollection
    @CollectionTable(name = "alcohol_request_alias", joinColumns = @JoinColumn(name = "request_id"))
    @Column(name = "alias")
    @Builder.Default
    private List<String> aliases = new ArrayList<>();

    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AlcoholRequestStatus status = AlcoholRequestStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlcoholCategory category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merged_to_alcohol_id")
    private Alcohol mergedToAlcohol;

    public void approve() {
        this.status = AlcoholRequestStatus.APPROVED;
    }

    public void merge(Alcohol alcohol) {
        this.status = AlcoholRequestStatus.MERGED;
        this.mergedToAlcohol = alcohol;
    }

    public void reject() {
        this.status = AlcoholRequestStatus.REJECTED;
    }
}