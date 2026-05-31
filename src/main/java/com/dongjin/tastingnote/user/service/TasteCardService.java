package com.dongjin.tastingnote.user.service;

import com.dongjin.tastingnote.note.entity.Note;
import com.dongjin.tastingnote.note.repository.NoteRepository;
import com.dongjin.tastingnote.user.dto.TasteCardResponse;
import com.dongjin.tastingnote.user.dto.TasteCardResponse.RatingGroup;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TasteCardService {

    private final NoteRepository noteRepository;

    public TasteCardResponse getTasteCard(Long userId) {
        List<Note> notes = noteRepository.findPublishedNotesForTasteCard(userId);

        // 별점 내림차순(역순 TreeMap)으로 그룹핑
        Map<BigDecimal, List<String>> grouped = notes.stream()
                .collect(Collectors.groupingBy(
                        Note::getRating,
                        () -> new TreeMap<>(Comparator.reverseOrder()),
                        Collectors.mapping(this::resolveAlcoholName, Collectors.toList())
                ));

        List<RatingGroup> ratings = grouped.entrySet().stream()
                .map(e -> new RatingGroup(e.getKey(), e.getValue()))
                .toList();

        return new TasteCardResponse(ratings);
    }

    // alcohol.nameKo → alcohol.name → customAlcoholName 우선순위
    private String resolveAlcoholName(Note note) {
        if (note.getAlcohol() != null) {
            String nameKo = note.getAlcohol().getNameKo();
            if (nameKo != null && !nameKo.isBlank()) return nameKo;
            return note.getAlcohol().getName();
        }
        return note.getCustomAlcoholName();
    }
}