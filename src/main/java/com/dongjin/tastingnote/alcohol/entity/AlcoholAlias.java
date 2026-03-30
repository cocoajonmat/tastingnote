package com.dongjin.tastingnote.alcohol.entity;

import com.dongjin.tastingnote.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "alcohol_alias")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AlcoholAlias extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alcohol_id", nullable = false)
    private Alcohol alcohol;

    @Column(nullable = false)
    private String alias;
}
