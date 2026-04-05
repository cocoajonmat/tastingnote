package com.dongjin.tastingnote.note.repository;

import com.dongjin.tastingnote.note.entity.NoteFlavor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NoteFlavorRepository extends JpaRepository<NoteFlavor, Long> {

    List<NoteFlavor> findAllByNoteId(Long noteId);

    void deleteAllByNoteId(Long noteId);
}