package com.dongjin.tastingnote.note.repository;

import com.dongjin.tastingnote.note.entity.Note;
import com.dongjin.tastingnote.note.entity.NoteStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NoteRepository extends JpaRepository<Note, Long> {

    // 내 노트 전체 조회 (임시저장 + 발행 모두)
    List<Note> findAllByUserId(Long userId);

    // 내 노트 상태별 조회 (임시저장만 or 발행만)
    List<Note> findAllByUserIdAndStatus(Long userId, NoteStatus status);

    // 공개된 발행 노트 전체 조회 (소셜 피드용)
    List<Note> findAllByIsPublicTrueAndStatus(NoteStatus status);
}