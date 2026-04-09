package com.dongjin.tastingnote.note.dto;

import com.dongjin.tastingnote.note.entity.FlavorType;
import com.dongjin.tastingnote.note.entity.Note;
import com.dongjin.tastingnote.note.entity.NoteFlavor;
import com.dongjin.tastingnote.note.entity.NoteStatus;
import lombok.Builder;
import lombok.Getter;

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
    private List<String> tastes;
    private List<String> aromas;
    private String pairing;
    private Double rating;
    private String description;
    private Boolean isPublic;
    private NoteStatus status;
    private LocalDate drankAt;
    private String location;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static NoteResponse from(Note note, List<NoteFlavor> flavors) {
        List<String> tastes = flavors.stream()
                .filter(f -> f.getType() == FlavorType.TASTE)
                .map(f -> f.getFlavor().getName())
                .toList();

        List<String> aromas = flavors.stream()
                .filter(f -> f.getType() == FlavorType.AROMA)
                .map(f -> f.getFlavor().getName())
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