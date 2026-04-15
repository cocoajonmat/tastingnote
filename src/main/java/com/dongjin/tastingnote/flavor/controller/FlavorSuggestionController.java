package com.dongjin.tastingnote.flavor.controller;

import com.dongjin.tastingnote.flavor.dto.FlavorSuggestionResponse;
import com.dongjin.tastingnote.flavor.service.FlavorSuggestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "맛/향 제안", description = "노트 작성 시 taste/aroma 입력 제안 목록 API")
@RestController
@RequestMapping("/api/flavors")
@RequiredArgsConstructor
public class FlavorSuggestionController {

    private final FlavorSuggestionService flavorSuggestionService;

    @Operation(summary = "맛/향 제안 목록 조회", description = "노트 작성 시 taste, aroma 입력에 사용할 제안 목록을 반환합니다.")
    @GetMapping
    public ResponseEntity<List<FlavorSuggestionResponse>> getAll() {
        return ResponseEntity.ok(flavorSuggestionService.getAll());
    }
}
