package com.dongjin.tastingnote.note.repository;

import com.dongjin.tastingnote.note.entity.Note;
import com.dongjin.tastingnote.note.entity.NoteStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface NoteRepository extends JpaRepository<Note, Long> {

    @Query("SELECT n FROM Note n LEFT JOIN FETCH n.alcohol JOIN FETCH n.user WHERE n.id = :noteId")
    Optional<Note> findByIdWithAlcoholAndUser(@Param("noteId") Long noteId);

    // ── 공개 피드 Latest ─────────────────────────────────────────────────
    @Query("SELECT n FROM Note n LEFT JOIN FETCH n.alcohol JOIN FETCH n.user " +
           "WHERE n.isPublic = true AND n.status = :status AND n.id < :cursor " +
           "ORDER BY n.id DESC")
    List<Note> findPublicLatestByCursor(
            @Param("cursor") Long cursor,
            @Param("status") NoteStatus status,
            Pageable pageable);

    // ── 공개 피드 Popular ────────────────────────────────────────────────
    @Query("SELECT n FROM Note n LEFT JOIN FETCH n.alcohol JOIN FETCH n.user " +
           "WHERE n.isPublic = true AND n.status = :status " +
           "AND (n.likeCount < :likeCount OR (n.likeCount = :likeCount AND n.id < :id)) " +
           "ORDER BY n.likeCount DESC, n.id DESC")
    List<Note> findPublicPopularByCursor(
            @Param("likeCount") int likeCount,
            @Param("id") long id,
            @Param("status") NoteStatus status,
            Pageable pageable);

    // ── 공개 피드 Hot (2단계: ID만 조회) ──────────────────────────────────
    @Query(value = """
            SELECT n.id FROM note n
            WHERE n.is_public = TRUE AND n.status = 'PUBLISHED'
              AND (n.like_count / POWER(TIMESTAMPDIFF(HOUR, n.created_at, NOW()) + 2, 1.5) < :hotScore
                   OR (n.like_count / POWER(TIMESTAMPDIFF(HOUR, n.created_at, NOW()) + 2, 1.5) = :hotScore AND n.id < :id))
            ORDER BY n.like_count / POWER(TIMESTAMPDIFF(HOUR, n.created_at, NOW()) + 2, 1.5) DESC, n.id DESC
            LIMIT :size
            """, nativeQuery = true)
    List<Long> findPublicHotIdsByCursor(
            @Param("hotScore") double hotScore,
            @Param("id") long id,
            @Param("size") int size);

    // ── 엔티티 일괄 조회 (hot 2단계용) ───────────────────────────────────
    @Query("SELECT n FROM Note n LEFT JOIN FETCH n.alcohol JOIN FETCH n.user WHERE n.id IN :ids")
    List<Note> findByIdInWithAlcoholAndUser(@Param("ids") List<Long> ids);

    // ── 내 노트 Offset (전체) ─────────────────────────────────────────────
    @Query(value = "SELECT n FROM Note n LEFT JOIN FETCH n.alcohol JOIN FETCH n.user " +
                   "WHERE n.user.id = :userId ORDER BY n.id DESC",
           countQuery = "SELECT COUNT(n) FROM Note n WHERE n.user.id = :userId")
    Page<Note> findMyNotesPaged(@Param("userId") Long userId, Pageable pageable);

    // ── 내 노트 Offset (상태 필터) ────────────────────────────────────────
    @Query(value = "SELECT n FROM Note n LEFT JOIN FETCH n.alcohol JOIN FETCH n.user " +
                   "WHERE n.user.id = :userId AND n.status = :status ORDER BY n.id DESC",
           countQuery = "SELECT COUNT(n) FROM Note n WHERE n.user.id = :userId AND n.status = :status")
    Page<Note> findMyNotesPagedByStatus(
            @Param("userId") Long userId,
            @Param("status") NoteStatus status,
            Pageable pageable);
}