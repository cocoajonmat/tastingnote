package com.dongjin.tastingnote.note.entity;

import com.dongjin.tastingnote.common.BaseEntity;
import com.dongjin.tastingnote.flavor.entity.FlavorSuggestion;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "note_flavor",
        uniqueConstraints = @UniqueConstraint(columnNames = {"note_id", "flavor_id", "type"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class NoteFlavor extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "note_id", nullable = false)
    private Note note;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flavor_id", nullable = false)
    private FlavorSuggestion flavor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FlavorType type;
}