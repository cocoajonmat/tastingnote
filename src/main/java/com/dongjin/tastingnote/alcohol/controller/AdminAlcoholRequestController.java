package com.dongjin.tastingnote.alcohol.controller;

import com.dongjin.tastingnote.alcohol.dto.AlcoholRequestResponse;
import com.dongjin.tastingnote.alcohol.entity.AlcoholRequestStatus;
import com.dongjin.tastingnote.alcohol.service.AlcoholRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "관리자 - 술 등록 요청", description = "술 등록 요청 관리자 API (ADMIN 전용)")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/admin/alcohol-requests")
@RequiredArgsConstructor
public class AdminAlcoholRequestController {

    private final AlcoholRequestService alcoholRequestService;

    @Operation(summary = "등록 요청 목록 조회", description = "상태별 술 등록 요청 목록을 조회합니다. 유사 술 목록도 함께 반환합니다.")
    @GetMapping
    public ResponseEntity<List<AlcoholRequestResponse>> getRequests(
            @RequestParam(defaultValue = "PENDING") AlcoholRequestStatus status) {
        return ResponseEntity.ok(alcoholRequestService.getRequests(status));
    }

    @Operation(summary = "등록 요청 승인", description = "요청된 술을 새로운 Alcohol로 등록합니다.")
    @PostMapping("/{id}/approve")
    public ResponseEntity<Void> approve(@PathVariable Long id) {
        alcoholRequestService.approve(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "등록 요청 병합", description = "요청된 술을 기존 Alcohol의 별칭으로 추가합니다.")
    @PostMapping("/{id}/merge")
    public ResponseEntity<Void> merge(
            @PathVariable Long id,
            @RequestParam Long alcoholId) {
        alcoholRequestService.merge(id, alcoholId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "등록 요청 거절", description = "등록 요청을 거절합니다. rejectReason으로 거절 사유를 전달할 수 있습니다.")
    @PostMapping("/{id}/reject")
    public ResponseEntity<Void> reject(
            @PathVariable Long id,
            @RequestParam(required = false) String rejectReason) {
        alcoholRequestService.reject(id, rejectReason);
        return ResponseEntity.noContent().build();
    }
}