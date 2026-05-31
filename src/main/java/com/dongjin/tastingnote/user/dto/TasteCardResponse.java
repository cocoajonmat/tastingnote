package com.dongjin.tastingnote.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@AllArgsConstructor
public class TasteCardResponse {

    private List<RatingGroup> ratings;

    @Getter
    @AllArgsConstructor
    public static class RatingGroup {
        private BigDecimal rating;
        private List<String> alcoholNames;
    }
}