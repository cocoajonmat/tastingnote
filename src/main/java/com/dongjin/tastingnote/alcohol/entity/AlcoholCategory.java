package com.dongjin.tastingnote.alcohol.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AlcoholCategory {
    WHISKEY("위스키"),
    WINE("와인"),
    BEER("맥주"),
    SOJU("소주"),
    MAKGEOLLI("막걸리"),
    SAKE("사케"),
    VODKA("보드카"),
    GIN("진"),
    RUM("럼"),
    TEQUILA("테킬라"),
    BRANDY("브랜디"),
    COCKTAIL("칵테일"),
    ETC("기타");

    private final String nameKo;

    public static AlcoholCategory findByNameKo(String keyword) {
        for (AlcoholCategory category : values()) {
            if (category.nameKo.contains(keyword) || keyword.contains(category.nameKo)) {
                return category;
            }
        }
        return null;
    }
}
