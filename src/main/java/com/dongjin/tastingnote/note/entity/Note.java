package com.dongjin.tastingnote.note.entity;

import com.dongjin.tastingnote.alcohol.entity.Alcohol;
import com.dongjin.tastingnote.common.BaseEntity;
import com.dongjin.tastingnote.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

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
    @JoinColumn(name = "alcohol_id", nullable = false)
    private Alcohol alcohol;

    @Column(nullable = false)
    private String title;

    private String pairing;

    @Column(columnDefinition = "DECIMAL(2,1)")
    private Double rating;

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
    public void update(Alcohol alcohol, String title, String pairing, Double rating, String description,
                       Boolean isPublic, LocalDate drankAt, String location) {
        this.alcohol = alcohol;
        this.title = title;
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

    // 임시저장으로 되돌리기
    public void saveDraft() {
        this.status = NoteStatus.DRAFT;
        this.isPublic = false;
    }
}