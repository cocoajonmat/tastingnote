package com.dongjin.tastingnote.alcohol.dto;

import com.dongjin.tastingnote.alcohol.entity.AlcoholCategory;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.util.List;

@Getter
public class AlcoholRequestCreateRequest {

    @Size(max = 100)
    private String name;

    @Size(max = 100)
    private String nameKo;

    @Size(max = 10)
    private List<String> aliases;

    @Size(max = 500)
    private String reason;

    private AlcoholCategory category;
}