package com.dongjin.tastingnote.flavor.dto;

import com.dongjin.tastingnote.flavor.entity.FlavorSuggestion;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FlavorSuggestionResponse {

    private Long id;
    private String name;

    public static FlavorSuggestionResponse from(FlavorSuggestion flavor) {
        return FlavorSuggestionResponse.builder()
                .id(flavor.getId())
                .name(flavor.getName())
                .build();
    }
}