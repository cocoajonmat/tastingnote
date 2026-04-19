package com.dongjin.tastingnote.alcohol.controller;

import com.dongjin.tastingnote.alcohol.dto.AlcoholAliasCreateRequest;
import com.dongjin.tastingnote.alcohol.dto.AlcoholRequestCreateRequest;
import com.dongjin.tastingnote.alcohol.service.AlcoholRequestService;
import com.dongjin.tastingnote.common.resolver.CurrentUserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "술 등록 요청", description = "DB에 없는 술 등록 요청 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/alcohol-requests")
@RequiredArgsConstructor
public class AlcoholRequestController {

    private final AlcoholRequestService alcoholRequestService;

    @Operation(summary = "신규 술 등록 요청",
            description = "DB에 없는 술을 등록 요청합니다. name, nameKo 중 하나 이상 필수. 관리자가 검토 후 승인/거절 처리합니다.")
    @PostMapping
    public ResponseEntity<Void> request(
            @CurrentUserId Long userId,
            @Valid @RequestBody AlcoholRequestCreateRequest request) {
        alcoholRequestService.request(userId, request);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "별칭 추가 요청",
            description = "기존 술에 새로운 별칭을 추가 요청합니다. 관리자가 검토 후 승인/거절 처리합니다.")
    @PostMapping("/{alcoholId}/alias")
    public ResponseEntity<Void> requestAlias(
            @CurrentUserId Long userId,
            @PathVariable Long alcoholId,
            @Valid @RequestBody AlcoholAliasCreateRequest request) {
        alcoholRequestService.requestAlias(userId, alcoholId, request);
        return ResponseEntity.noContent().build();
    }
}