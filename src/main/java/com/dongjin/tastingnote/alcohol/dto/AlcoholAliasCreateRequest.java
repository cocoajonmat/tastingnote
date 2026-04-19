package com.dongjin.tastingnote.alcohol.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class AlcoholAliasCreateRequest {

    @NotBlank
    @Size(max = 100)
    private String alias;

    @Size(max = 500)
    private String reason;
}