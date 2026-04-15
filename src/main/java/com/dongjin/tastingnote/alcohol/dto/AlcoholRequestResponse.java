package com.dongjin.tastingnote.alcohol.dto;

import com.dongjin.tastingnote.alcohol.entity.AlcoholCategory;
import com.dongjin.tastingnote.alcohol.entity.AlcoholRequest;
import com.dongjin.tastingnote.alcohol.entity.AlcoholRequestStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class AlcoholRequestResponse {

    private Long id;
    private String name;
    private String nameKo;
    private List<String> aliases;
    private String reason;
    private AlcoholCategory category;
    private AlcoholRequestStatus status;
    private Long requestedById;
    private String requestedByNickname;
    private LocalDateTime createdAt;
    private List<AlcoholResponse> similarAlcohols;

    public static AlcoholRequestResponse from(AlcoholRequest request, List<AlcoholResponse> similarAlcohols) {
        return AlcoholRequestResponse.builder()
                .id(request.getId())
                .name(request.getName())
                .nameKo(request.getNameKo())
                .aliases(request.getAliases())
                .reason(request.getReason())
                .category(request.getCategory())
                .status(request.getStatus())
                .requestedById(request.getRequestedBy().getId())
                .requestedByNickname(request.getRequestedBy().getNickname())
                .createdAt(request.getCreatedAt())
                .similarAlcohols(similarAlcohols)
                .build();
    }
}