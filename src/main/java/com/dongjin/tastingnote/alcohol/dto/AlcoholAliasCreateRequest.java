package com.dongjin.tastingnote.alcohol.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.util.List;

@Getter
public class AlcoholAliasCreateRequest {

    @NotEmpty
    @Size(max = 10)
    private List<String> aliases;

    @Size(max = 500)
    private String reason;
}