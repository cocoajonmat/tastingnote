package com.dongjin.tastingnote.note.repository;

import com.dongjin.tastingnote.note.entity.NoteImage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoteImageRepository extends JpaRepository<NoteImage, Long> {

    void deleteAllByNoteId(Long noteId);
}