package com.dongjin.tastingnote.tag.repository;

import com.dongjin.tastingnote.tag.entity.NoteTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NoteTagRepository extends JpaRepository<NoteTag, Long> {

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM NoteTag nt WHERE nt.note.id = :noteId")
    void deleteAllByNoteId(@Param("noteId") Long noteId);
}