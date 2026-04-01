package com.dongjin.tastingnote.report.controller;

import com.dongjin.tastingnote.common.response.ApiResponse;
import com.dongjin.tastingnote.report.dto.ReportRequest;
import com.dongjin.tastingnote.report.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notes/{noteId}/report")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> report(
            @PathVariable Long noteId,
            @Valid @RequestBody ReportRequest request
    ) {
        Long reporterId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        reportService.report(reporterId, noteId, request);
        return ResponseEntity.ok(ApiResponse.ok());
    }
}