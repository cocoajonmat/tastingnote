package com.dongjin.tastingnote.note.repository;

import com.dongjin.tastingnote.note.entity.NoteFlavor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NoteFlavorRepository extends JpaRepository<NoteFlavor, Long> {

    List<NoteFlavor> findAllByNoteId(Long noteId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM NoteFlavor nf WHERE nf.note.id = :noteId")
    void deleteAllByNoteId(@Param("noteId") Long noteId);
}