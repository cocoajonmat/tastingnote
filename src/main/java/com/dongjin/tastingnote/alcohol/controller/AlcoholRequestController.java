package com.dongjin.tastingnote.alcohol.controller;

import com.dongjin.tastingnote.alcohol.dto.AlcoholRequestCreateRequest;
import com.dongjin.tastingnote.alcohol.service.AlcoholRequestService;
import com.dongjin.tastingnote.common.resolver.CurrentUserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "술 등록 요청", description = "DB에 없는 술 등록 요청 API")
@RestController
@RequestMapping("/api/alcohol-requests")
@RequiredArgsConstructor
public class AlcoholRequestController {

    private final AlcoholRequestService alcoholRequestService;

    @Operation(summary = "술 등록 요청", description = "DB에 없는 술을 등록 요청합니다. 관리자가 검토 후 승인/병합/거절 처리합니다.")
    @PostMapping
    public ResponseEntity<Void> request(
            @CurrentUserId Long userId,
            @Valid @RequestBody AlcoholRequestCreateRequest request) {
        alcoholRequestService.request(userId, request);
        return ResponseEntity.ok().build();
    }
}