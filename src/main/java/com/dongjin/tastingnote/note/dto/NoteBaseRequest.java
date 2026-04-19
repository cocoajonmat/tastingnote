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

@Getter
@NoArgsConstructor
public abstract class NoteBaseRequest {

    private Long alcoholId;

    @Size(max = 100, message = "술 이름은 100자 이하여야 합니다")
    private String customAlcoholName;

    @NotBlank(message = "제목은 필수입니다")
    @Size(max = 100, message = "제목은 100자 이하여야 합니다")
    private String title;

    private String taste;

    private String aroma;

    private String pairing;

    @NotNull(message = "별점은 필수입니다")
    @DecimalMin(value = "0.5", message = "별점은 0.5 이상이어야 합니다")
    @DecimalMax(value = "5.0", message = "별점은 5.0 이하이어야 합니다")
    private BigDecimal rating;

    private String description;

    private LocalDate drankAt;

    @Size(max = 100, message = "장소는 100자 이하여야 합니다")
    private String location;

    // isPublic은 Create(기본값 false)와 Update(@NotNull)의 동작이 달라서 서브클래스에서 각자 선언
    public abstract Boolean getIsPublic();
}
