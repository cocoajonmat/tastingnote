package com.dongjin.tastingnote.alcohol.dto;

import com.dongjin.tastingnote.alcohol.entity.AlcoholCategory;
import com.dongjin.tastingnote.alcohol.entity.AlcoholRequest;
import com.dongjin.tastingnote.alcohol.entity.AlcoholRequestStatus;
import com.dongjin.tastingnote.alcohol.entity.AlcoholRequestType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class AlcoholRequestResponse {

    private Long id;
    private AlcoholRequestType type;
    private String name;
    private String nameKo;
    private List<String> aliases;
    private String reason;
    private String rejectReason;
    private AlcoholCategory category;
    private AlcoholRequestStatus status;
    private Long requestedById;
    private String requestedByNickname;
    private Long targetAlcoholId;
    private String targetAlcoholName;
    private LocalDateTime createdAt;
    private List<AlcoholResponse> similarAlcohols;

    public static AlcoholRequestResponse from(AlcoholRequest request, List<AlcoholResponse> similarAlcohols) {
        return AlcoholRequestResponse.builder()
                .id(request.getId())
                .type(request.getType())
                .name(request.getName())
                .nameKo(request.getNameKo())
                .aliases(request.getAliases())
                .reason(request.getReason())
                .rejectReason(request.getRejectReason())
                .category(request.getCategory())
                .status(request.getStatus())
                .requestedById(request.getRequestedBy().getId())
                .requestedByNickname(request.getRequestedBy().getNickname())
                .targetAlcoholId(request.getTargetAlcohol() != null ? request.getTargetAlcohol().getId() : null)
                .targetAlcoholName(request.getTargetAlcohol() != null ? request.getTargetAlcohol().getName() : null)
                .createdAt(request.getCreatedAt())
                .similarAlcohols(similarAlcohols)
                .build();
    }
}