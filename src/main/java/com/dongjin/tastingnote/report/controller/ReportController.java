package com.dongjin.tastingnote.report.controller;

import com.dongjin.tastingnote.common.resolver.CurrentUserId;
import com.dongjin.tastingnote.report.dto.ReportRequest;
import com.dongjin.tastingnote.report.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "신고", description = "노트 신고 관련 API")
@RestController
@RequestMapping("/api/notes/{noteId}/report")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @Operation(summary = "노트 신고", description = "특정 노트를 신고합니다. 같은 노트를 중복 신고할 수 없습니다. 사유가 OTHER일 경우 reasonDetail을 함께 입력하세요.")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    public ResponseEntity<Void> report(
            @CurrentUserId Long reporterId,
            @PathVariable Long noteId,
            @Valid @RequestBody ReportRequest request
    ) {
        reportService.report(reporterId, noteId, request);
        return ResponseEntity.noContent().build();
    }
}
