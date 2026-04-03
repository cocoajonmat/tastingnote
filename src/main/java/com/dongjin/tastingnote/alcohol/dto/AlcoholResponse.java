package com.dongjin.tastingnote.alcohol.dto;

import com.dongjin.tastingnote.alcohol.entity.Alcohol;
import com.dongjin.tastingnote.alcohol.entity.AlcoholCategory;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AlcoholResponse {

    private Long id;
    private String name;
    private String nameKo;
    private AlcoholCategory category;
    private String origin;
    private String region;
    private Integer vintage;
    private Double abv;
    private String description;

    public static AlcoholResponse from(Alcohol alcohol) {
        return AlcoholResponse.builder()
                .id(alcohol.getId())
                .name(alcohol.getName())
                .nameKo(alcohol.getNameKo())
                .category(alcohol.getCategory())
                .origin(alcohol.getOrigin())
                .region(alcohol.getRegion())
                .vintage(alcohol.getVintage())
                .abv(alcohol.getAbv())
                .description(alcohol.getDescription())
                .build();
    }
}