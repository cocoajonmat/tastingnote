package com.dongjin.tastingnote.flavor.service;

import com.dongjin.tastingnote.flavor.dto.FlavorSuggestionResponse;
import com.dongjin.tastingnote.flavor.repository.FlavorSuggestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FlavorSuggestionService {

    private final FlavorSuggestionRepository flavorSuggestionRepository;

    public List<FlavorSuggestionResponse> getAll() {
        return flavorSuggestionRepository.findAll().stream()
                .map(FlavorSuggestionResponse::from)
                .toList();
    }
}
