package com.dongjin.tastingnote.note.dto;

import com.dongjin.tastingnote.note.entity.FlavorType;
import com.dongjin.tastingnote.note.entity.Note;
import com.dongjin.tastingnote.note.entity.NoteFlavor;
import com.dongjin.tastingnote.note.entity.NoteImage;
import com.dongjin.tastingnote.note.entity.NoteStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class NoteResponse {

    private Long id;
    private Long userId;
    private Long alcoholId;
    private String alcoholName;
    private String alcoholNameKo;
    private String title;
    private List<FlavorItem> tastes;
    private List<FlavorItem> aromas;
    private List<String> imageUrls;
    private String pairing;
    private BigDecimal rating;
    private String description;
    private Boolean isPublic;
    private NoteStatus status;
    private LocalDate drankAt;
    private String location;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Getter
    @Builder
    public static class FlavorItem {
        private Long id;
        private String name;
    }

    public static NoteResponse from(Note note, List<NoteFlavor> flavors, List<NoteImage> images) {
        List<FlavorItem> tastes = flavors.stream()
                .filter(f -> f.getType() == FlavorType.TASTE)
                .map(f -> FlavorItem.builder()
                        .id(f.getFlavor().getId())
                        .name(f.getFlavor().getName())
                        .build())
                .toList();

        List<FlavorItem> aromas = flavors.stream()
                .filter(f -> f.getType() == FlavorType.AROMA)
                .map(f -> FlavorItem.builder()
                        .id(f.getFlavor().getId())
                        .name(f.getFlavor().getName())
                        .build())
                .toList();

        return NoteResponse.builder()
                .id(note.getId())
                .userId(note.getUser().getId())
                .alcoholId(note.getAlcohol().getId())
                .alcoholName(note.getAlcohol().getName())
                .alcoholNameKo(note.getAlcohol().getNameKo())
                .title(note.getTitle())
                .tastes(tastes)
                .aromas(aromas)
                .imageUrls(images.stream().map(NoteImage::getImageUrl).toList())
                .pairing(note.getPairing())
                .rating(note.getRating())
                .description(note.getDescription())
                .isPublic(note.getIsPublic())
                .status(note.getStatus())
                .drankAt(note.getDrankAt())
                .location(note.getLocation())
                .createdAt(note.getCreatedAt())
                .updatedAt(note.getUpdatedAt())
                .build();
    }
}