package com.dongjin.tastingnote.alcohol.entity;

import com.dongjin.tastingnote.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "alcohol")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Alcohol extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "name_ko")
    private String nameKo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlcoholCategory category;

    private String origin;

    private String region;

    private Integer vintage;

    private Double abv;

    @Column(columnDefinition = "TEXT")
    private String description;
}
