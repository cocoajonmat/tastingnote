package com.dongjin.tastingnote.note.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor
public class NoteUpdateRequest {

    @NotNull(message = "술은 필수입니다")
    private Long alcoholId;

    @NotBlank(message = "제목은 필수입니다")
    @Size(max = 100, message = "제목은 100자 이하여야 합니다")
    private String title;

    @NotNull(message = "tasteIds는 null일 수 없습니다. 선택하지 않으려면 빈 배열([])을 보내주세요")
    private List<Long> tasteIds = new ArrayList<>();

    @NotNull(message = "aromaIds는 null일 수 없습니다. 선택하지 않으려면 빈 배열([])을 보내주세요")
    private List<Long> aromaIds = new ArrayList<>();
    private String pairing;

    @NotNull(message = "별점은 필수입니다")
    @DecimalMin(value = "0.5", message = "별점은 0.5 이상이어야 합니다")
    @DecimalMax(value = "5.0", message = "별점은 5.0 이하이어야 합니다")
    private BigDecimal rating;

    private String description;
    @NotNull(message = "isPublic은 null일 수 없습니다")
    private Boolean isPublic;
    private LocalDate drankAt;
    @Size(max = 100, message = "장소는 100자 이하여야 합니다")
    private String location;
}