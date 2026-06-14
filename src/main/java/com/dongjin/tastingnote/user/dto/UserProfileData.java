package com.dongjin.tastingnote.user.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record UserProfileData(
        int totalNotes,
        Map<String, Double> categoryScores,
        List<TopAlcohol> topRatedAlcohols,
        List<String> recentSearchKeywords,
        LocalDateTime updatedAt
) {
    public record TopAlcohol(
            String name,
            String nameKo,
            BigDecimal rating
    ) {}
}
