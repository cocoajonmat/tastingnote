package com.dongjin.tastingnote.note.entity;

import com.dongjin.tastingnote.common.BaseEntity;
import com.dongjin.tastingnote.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "note_like",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "note_id"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Like extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "note_id", nullable = false)
    private Note note;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LikeType type;
}
