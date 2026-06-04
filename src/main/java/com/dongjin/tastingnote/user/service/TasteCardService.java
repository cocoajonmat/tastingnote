package com.dongjin.tastingnote.user.service;

import com.dongjin.tastingnote.note.entity.Note;
import com.dongjin.tastingnote.note.repository.NoteRepository;
import com.dongjin.tastingnote.user.dto.TasteCardResponse;
import com.dongjin.tastingnote.user.dto.TasteCardResponse.RatingGroup;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TasteCardService {

    private final NoteRepository noteRepository;

    public TasteCardResponse getTasteCardGroupedByRating(Long userId) {
        List<Note> notes = noteRepository.findPublishedNotesByUserId(userId);

        Map<BigDecimal, List<String>> grouped = notes.stream()
                .collect(Collectors.groupingBy(
                        Note::getRating,
                        LinkedHashMap::new,
                        Collectors.mapping(Note::getAlcoholDisplayName, Collectors.toList())
                ));

        List<RatingGroup> ratings = grouped.entrySet().stream()
                .map(e -> new RatingGroup(e.getKey(), e.getValue()))
                .toList();

        return new TasteCardResponse(ratings);
    }
}