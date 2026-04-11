package com.dongjin.tastingnote.note.repository;

import com.dongjin.tastingnote.note.entity.NoteImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NoteImageRepository extends JpaRepository<NoteImage, Long> {

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM NoteImage ni WHERE ni.note.id = :noteId")
    void deleteAllByNoteId(@Param("noteId") Long noteId);
}