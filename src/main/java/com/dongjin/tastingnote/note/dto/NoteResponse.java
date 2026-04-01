package com.dongjin.tastingnote.note.dto;

import com.dongjin.tastingnote.note.entity.Note;
import com.dongjin.tastingnote.note.entity.NoteStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class NoteResponse {

    private Long id;
    private Long userId;
    private Long alcoholId;
    private String alcoholName;
    private String title;
    private String taste;
    private String aroma;
    private String pairing;
    private Double rating;
    private String description;
    private Boolean isPublic;
    private NoteStatus status;
    private LocalDate drankAt;
    private String location;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static NoteResponse from(Note note) {
        return NoteResponse.builder()
                .id(note.getId())
                .userId(note.getUser().getId())
                .alcoholId(note.getAlcohol() != null ? note.getAlcohol().getId() : null)
                .alcoholName(note.getAlcoholName())
                .title(note.getTitle())
                .taste(note.getTaste())
                .aroma(note.getAroma())
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