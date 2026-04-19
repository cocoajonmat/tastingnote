package com.dongjin.tastingnote.note.entity;

import com.dongjin.tastingnote.alcohol.entity.Alcohol;
import com.dongjin.tastingnote.common.BaseEntity;
import com.dongjin.tastingnote.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "note")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Note extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alcohol_id")
    private Alcohol alcohol;

    @Column(name = "custom_alcohol_name")
    private String customAlcoholName;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String taste;

    @Column(columnDefinition = "TEXT")
    private String aroma;

    @Column(columnDefinition = "TEXT")
    private String pairing;

    @Column(nullable = false, precision = 2, scale = 1)
    private BigDecimal rating;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_public", nullable = false)
    @Builder.Default
    private Boolean isPublic = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private NoteStatus status = NoteStatus.DRAFT;

    @Column(name = "drank_at")
    private LocalDate drankAt;

    private String location;

    // 노트 내용 수정
    public void update(Alcohol alcohol, String customAlcoholName, String title, String taste, String aroma, String pairing,
                       BigDecimal rating, String description, Boolean isPublic, LocalDate drankAt, String location) {
        this.alcohol = alcohol;
        this.customAlcoholName = alcohol == null ? customAlcoholName : null;
        this.title = title;
        this.taste = taste;
        this.aroma = aroma;
        this.pairing = pairing;
        this.rating = rating;
        this.description = description;
        this.isPublic = isPublic;
        this.drankAt = drankAt;
        this.location = location;
    }

    // 발행 (임시저장 → 발행)
    public void publish() {
        this.status = NoteStatus.PUBLISHED;
    }
}