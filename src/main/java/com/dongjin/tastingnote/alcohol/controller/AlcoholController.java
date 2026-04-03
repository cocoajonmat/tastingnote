package com.dongjin.tastingnote.alcohol.controller;

import com.dongjin.tastingnote.alcohol.dto.AlcoholResponse;
import com.dongjin.tastingnote.alcohol.entity.AlcoholCategory;
import com.dongjin.tastingnote.alcohol.service.AlcoholService;
import com.dongjin.tastingnote.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "술", description = "술 검색 및 조회 API")
@RestController
@RequestMapping("/api/alcohols")
@RequiredArgsConstructor
public class AlcoholController {

    private final AlcoholService alcoholService;

    @Operation(summary = "술 검색", description = "키워드로 술을 검색합니다. 영문명, 한글명, 별칭(AlcoholAlias)을 통합 검색합니다.")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<AlcoholResponse>>> search(@RequestParam String keyword) {
        return ResponseEntity.ok(ApiResponse.ok(alcoholService.search(keyword)));
    }

    @Operation(summary = "카테고리별 술 목록", description = "카테고리로 술 목록을 조회합니다. 예: WHISKEY, WINE, BEER")
    @GetMapping
    public ResponseEntity<ApiResponse<List<AlcoholResponse>>> getByCategory(@RequestParam AlcoholCategory category) {
        return ResponseEntity.ok(ApiResponse.ok(alcoholService.getByCategory(category)));
    }

    @Operation(summary = "술 단건 조회", description = "술 ID로 상세 정보를 조회합니다.")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AlcoholResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(alcoholService.getById(id)));
    }
}