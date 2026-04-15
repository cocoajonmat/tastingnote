package com.dongjin.tastingnote.alcohol.dto;

import com.dongjin.tastingnote.alcohol.entity.AlcoholCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.util.List;

@Getter
public class AlcoholRequestCreateRequest {

    @NotBlank
    private String name;

    private String nameKo;

    @Size(max = 10)
    private List<String> aliases;

    @Size(max = 500)
    private String reason;

    @NotNull
    private AlcoholCategory category;
}