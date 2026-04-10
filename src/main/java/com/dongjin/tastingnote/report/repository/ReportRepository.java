package com.dongjin.tastingnote.report.repository;

import com.dongjin.tastingnote.report.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, Long> {

    boolean existsByReporterIdAndNoteId(Long reporterId, Long noteId);

    void deleteAllByNoteId(Long noteId);
}