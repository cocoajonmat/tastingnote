package com.dongjin.tastingnote.note.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor
public class NoteCreateRequest {

    @NotNull(message = "술은 필수입니다")
    private Long alcoholId;

    @NotBlank(message = "제목은 필수입니다")
    @Size(max = 100, message = "제목은 100자 이하여야 합니다")
    private String title;

    @NotNull(message = "tasteIds는 null일 수 없습니다. 선택하지 않으려면 빈 배열([])을 보내주세요")
    private List<Long> tasteIds = new ArrayList<>();   // FlavorSuggestion ID 목록 (맛)

    @NotNull(message = "aromaIds는 null일 수 없습니다. 선택하지 않으려면 빈 배열([])을 보내주세요")
    private List<Long> aromaIds = new ArrayList<>();   // FlavorSuggestion ID 목록 (향)
    private String pairing;

    @NotNull(message = "별점은 필수입니다")
    @Min(value = 1, message = "별점은 1.0 이상이어야 합니다")
    @Max(value = 5, message = "별점은 5.0 이하이어야 합니다")
    private Double rating;

    private String description;
    private Boolean isPublic = false;
    private LocalDate drankAt;
    @Size(max = 100, message = "장소는 100자 이하여야 합니다")
    private String location;
}