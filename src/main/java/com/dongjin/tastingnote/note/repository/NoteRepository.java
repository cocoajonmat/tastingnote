package com.dongjin.tastingnote.note.repository;

import com.dongjin.tastingnote.note.entity.Note;
import com.dongjin.tastingnote.note.entity.NoteStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface NoteRepository extends JpaRepository<Note, Long> {

	// todo: 지연로딩 문제로 이미지 객체 불러오는 부분에서 에러 발생.
	// todo: 임시로 fetch 조인으로 수정했지만, 이후 정확한 원인 파악 후 해결 필요
    @Query("SELECT n FROM Note n LEFT JOIN FETCH n.alcohol JOIN FETCH n.user WHERE n.id = :noteId")
    Optional<Note> findByIdWithAlcoholAndUser(@Param("noteId") Long noteId);

    // 내 노트 전체 조회 (임시저장 + 발행 모두) — 최신순
    List<Note> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    // 내 노트 상태별 조회 (임시저장만 or 발행만) — 최신순
    List<Note> findAllByUserIdAndStatusOrderByCreatedAtDesc(Long userId, NoteStatus status);

    // 공개된 발행 노트 전체 조회 (소셜 피드용) — 최신순
    List<Note> findAllByIsPublicTrueAndStatusOrderByCreatedAtDesc(NoteStatus status);
}