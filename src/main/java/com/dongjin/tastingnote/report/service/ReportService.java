package com.dongjin.tastingnote.report.service;

import com.dongjin.tastingnote.common.exception.BusinessException;
import com.dongjin.tastingnote.common.exception.ErrorCode;
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
            throw new BusinessException(ErrorCode.ALREADY_REPORTED);
        }

        User reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTE_NOT_FOUND));

        if (note.getUser().getId().equals(reporterId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ACCESS);
        }

        reportRepository.save(Report.builder()
                .reporter(reporter)
                .note(note)
                .reason(request.getReason())
                .reasonDetail(request.getReasonDetail())
                .build());
    }
}