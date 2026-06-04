package com.dongjin.tastingnote.user.dto;

import java.math.BigDecimal;
import java.util.List;

public record TasteCardResponse(List<RatingGroup> ratings) {

    public record RatingGroup(BigDecimal rating, List<String> alcoholNames) {}
}