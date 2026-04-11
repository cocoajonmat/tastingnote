package com.dongjin.tastingnote.report.repository;

import com.dongjin.tastingnote.report.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReportRepository extends JpaRepository<Report, Long> {

    boolean existsByReporterIdAndNoteId(Long reporterId, Long noteId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Report r WHERE r.note.id = :noteId")
    void deleteAllByNoteId(@Param("noteId") Long noteId);
}