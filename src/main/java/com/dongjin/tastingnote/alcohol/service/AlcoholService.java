package com.dongjin.tastingnote.alcohol.service;

import com.dongjin.tastingnote.alcohol.dto.AlcoholResponse;
import com.dongjin.tastingnote.alcohol.entity.AlcoholCategory;
import com.dongjin.tastingnote.alcohol.repository.AlcoholRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AlcoholService {

    private final AlcoholRepository alcoholRepository;

    @Transactional(readOnly = true)
    public List<AlcoholResponse> search(String keyword) {
        return alcoholRepository.searchByKeyword(keyword).stream()
                .map(AlcoholResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AlcoholResponse> getByCategory(AlcoholCategory category) {
        return alcoholRepository.findAllByCategory(category).stream()
                .map(AlcoholResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AlcoholResponse getById(Long id) {
        return alcoholRepository.findById(id)
                .map(AlcoholResponse::from)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 술입니다"));
    }
}