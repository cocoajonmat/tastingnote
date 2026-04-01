package com.dongjin.tastingnote.report.service;

import com.dongjin.tastingnote.note.entity.Note;
import com.dongjin.tastingnote.note.repository.NoteRepository;
import com.dongjin.tastingnote.report.dto.ReportRequest;
import com.dongjin.tastingnote.report.entity.Report;
import com.dongjin.tastingnote.report.repository.ReportRepository;
import com.dongjin.tastingnote.user.entity.User;
import com.dongjin.tastingnote.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;
    private final NoteRepository noteRepository;
    private final UserRepository userRepository;

    @Transactional
    public void report(Long reporterId, Long noteId, ReportRequest request) {
        if (reportRepository.existsByReporterIdAndNoteId(reporterId, noteId)) {
            throw new IllegalArgumentException("이미 신고한 노트입니다");
        }

        User reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다"));

        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 노트입니다"));

        reportRepository.save(Report.builder()
                .reporter(reporter)
                .note(note)
                .reason(request.getReason())
                .reasonDetail(request.getReasonDetail())
                .build());
    }
}