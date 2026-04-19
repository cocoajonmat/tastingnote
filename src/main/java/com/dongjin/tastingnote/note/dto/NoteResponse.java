package com.dongjin.tastingnote.note.dto;

import com.dongjin.tastingnote.note.entity.Note;
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
    private String nickname;
    private Long alcoholId;
    private String alcoholName;
    private String alcoholNameKo;
    private String customAlcoholName;
    private String title;
    private String taste;
    private String aroma;
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

    public static NoteResponse from(Note note, List<NoteImage> images) {
        return NoteResponse.builder()
                .id(note.getId())
                .userId(note.getUser().getId())
                .nickname(note.getUser().getNickname())
                .alcoholId(note.getAlcohol() != null ? note.getAlcohol().getId() : null)
                .alcoholName(note.getAlcohol() != null ? note.getAlcohol().getName() : null)
                .alcoholNameKo(note.getAlcohol() != null ? note.getAlcohol().getNameKo() : null)
                .customAlcoholName(note.getCustomAlcoholName())
                .title(note.getTitle())
                .taste(note.getTaste())
                .aroma(note.getAroma())
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