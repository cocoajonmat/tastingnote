package com.dongjin.tastingnote.user.controller;

import com.dongjin.tastingnote.common.resolver.CurrentUserId;
import com.dongjin.tastingnote.user.dto.TasteCardResponse;
import com.dongjin.tastingnote.user.service.TasteCardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "취향 카드", description = "바텐더에게 보여주는 내 취향 카드 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class TasteCardController {

    private final TasteCardService tasteCardService;

    @Operation(summary = "바텐더 취향 카드 조회", description = "내 PUBLISHED 노트를 별점별로 묶어 술 이름 목록을 반환합니다. 별점 내림차순 정렬.")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/me/taste-card")
    public ResponseEntity<TasteCardResponse> getTasteCard(@CurrentUserId Long userId) {
        return ResponseEntity.ok(tasteCardService.getTasteCard(userId));
    }
}