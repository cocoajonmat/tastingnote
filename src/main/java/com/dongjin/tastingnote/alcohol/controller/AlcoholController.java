package com.dongjin.tastingnote.alcohol.controller;

import com.dongjin.tastingnote.alcohol.dto.AlcoholResponse;
import com.dongjin.tastingnote.alcohol.entity.AlcoholCategory;
import com.dongjin.tastingnote.alcohol.service.AlcoholService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "술", description = "술 검색 및 조회 API")
@RestController
@RequestMapping("/api/alcohols")
@RequiredArgsConstructor
@Validated
public class AlcoholController {

    private final AlcoholService alcoholService;

    @Operation(summary = "술 검색", description = "키워드로 술을 검색합니다. 영문명, 한글명, 별칭(AlcoholAlias)을 통합 검색합니다.")
    @GetMapping("/search")
    public ResponseEntity<List<AlcoholResponse>> search(
            @RequestParam @NotBlank(message = "검색어는 공백일 수 없습니다") @Size(min = 1, message = "검색어는 1자 이상 입력해주세요") String keyword) {
        return ResponseEntity.ok(alcoholService.search(keyword));
    }

    @Operation(summary = "카테고리별 술 목록", description = "카테고리로 술 목록을 조회합니다. 예: WHISKEY, WINE, BEER")
    @GetMapping
    public ResponseEntity<List<AlcoholResponse>> getByCategory(@RequestParam AlcoholCategory category) {
        return ResponseEntity.ok(alcoholService.getByCategory(category));
    }

    @Operation(summary = "술 단건 조회", description = "술 ID로 상세 정보를 조회합니다.")
    @GetMapping("/{id}")
    public ResponseEntity<AlcoholResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(alcoholService.getById(id));
    }
}
