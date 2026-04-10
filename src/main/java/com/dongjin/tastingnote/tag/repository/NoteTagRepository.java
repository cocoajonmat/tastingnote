package com.dongjin.tastingnote.tag.repository;

import com.dongjin.tastingnote.tag.entity.NoteTag;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoteTagRepository extends JpaRepository<NoteTag, Long> {

    void deleteAllByNoteId(Long noteId);
}