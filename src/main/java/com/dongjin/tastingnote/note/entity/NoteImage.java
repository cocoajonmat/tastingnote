package com.dongjin.tastingnote.note.entity;

import com.dongjin.tastingnote.common.BaseEntity;
import com.dongjin.tastingnote.note.entity.Note;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "note_image")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class NoteImage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "note_id", nullable = false)
    private Note note;

    @Column(name = "image_url", nullable = false)
    private String imageUrl;

    private String s3Key;
}