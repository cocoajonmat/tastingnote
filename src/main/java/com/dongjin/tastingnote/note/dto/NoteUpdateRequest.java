package com.dongjin.tastingnote.note.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class NoteUpdateRequest {

    @NotBlank(message = "제목은 필수입니다")
    private String title;

    private String taste;
    private String aroma;
    private String pairing;

    @Min(value = 1, message = "별점은 1 이상이어야 합니다")
    @Max(value = 5, message = "별점은 5 이하이어야 합니다")
    private Integer rating;

    private String description;
    private Boolean isPublic;
    private LocalDate drankAt;
    private String location;
}