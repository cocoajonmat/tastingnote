package com.dongjin.tastingnote.note.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class NoteCreateRequest {

    @NotNull(message = "userId는 필수입니다")
    private Long userId; // 임시 - 나중에 JWT로 교체

    private Long alcoholId;   // DB에 있는 술 선택 시
    private String alcoholName; // 직접 입력 시

    @NotBlank(message = "제목은 필수입니다")
    private String title;

    private String taste;
    private String aroma;
    private String pairing;

    @Min(value = 1, message = "별점은 1.0 이상이어야 합니다")
    @Max(value = 5, message = "별점은 5.0 이하이어야 합니다")
    private Double rating;

    private String description;
    private Boolean isPublic = false;
    private LocalDate drankAt;
    private String location;
}