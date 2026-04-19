package com.dongjin.tastingnote.alcohol.controller;

import com.dongjin.tastingnote.alcohol.dto.AlcoholRequestResponse;
import com.dongjin.tastingnote.alcohol.entity.AlcoholRequestStatus;
import com.dongjin.tastingnote.alcohol.entity.AlcoholRequestType;
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

    @Operation(summary = "등록 요청 목록 조회",
            description = "상태별 술 등록 요청 목록을 조회합니다. type=NEW|ALIAS로 필터링 가능합니다. 유사 술 목록도 함께 반환합니다.")
    @GetMapping
    public ResponseEntity<List<AlcoholRequestResponse>> getRequests(
            @RequestParam(defaultValue = "PENDING") AlcoholRequestStatus status,
            @RequestParam(required = false) AlcoholRequestType type) {
        return ResponseEntity.ok(alcoholRequestService.getRequests(status, type));
    }

    @Operation(summary = "신규 등록 요청 승인", description = "NEW 타입 요청을 승인하여 새로운 Alcohol로 등록합니다.")
    @PostMapping("/{id}/approve")
    public ResponseEntity<Void> approve(@PathVariable Long id) {
        alcoholRequestService.approve(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "별칭 추가 요청 승인", description = "ALIAS 타입 요청을 승인하여 기존 술에 별칭을 추가합니다.")
    @PostMapping("/{id}/approve-alias")
    public ResponseEntity<Void> approveAlias(@PathVariable Long id) {
        alcoholRequestService.approveAlias(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "등록 요청 거절", description = "NEW/ALIAS 요청 모두 거절할 수 있습니다. rejectReason으로 거절 사유를 전달할 수 있습니다.")
    @PostMapping("/{id}/reject")
    public ResponseEntity<Void> reject(
            @PathVariable Long id,
            @RequestParam(required = false) String rejectReason) {
        alcoholRequestService.reject(id, rejectReason);
        return ResponseEntity.noContent().build();
    }
}