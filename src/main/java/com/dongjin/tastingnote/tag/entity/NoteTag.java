package com.dongjin.tastingnote.tag.entity;

import com.dongjin.tastingnote.common.BaseEntity;
import com.dongjin.tastingnote.note.entity.Note;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "note_tag",
        uniqueConstraints = @UniqueConstraint(columnNames = {"note_id", "tag_id"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class NoteTag extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "note_id", nullable = false)
    private Note note;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;
}