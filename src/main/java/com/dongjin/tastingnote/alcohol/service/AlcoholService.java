package com.dongjin.tastingnote.alcohol.service;

import com.dongjin.tastingnote.alcohol.dto.AlcoholResponse;
import com.dongjin.tastingnote.alcohol.entity.AlcoholCategory;
import com.dongjin.tastingnote.alcohol.repository.AlcoholRepository;
import com.dongjin.tastingnote.common.exception.BusinessException;
import com.dongjin.tastingnote.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AlcoholService {

    private final AlcoholRepository alcoholRepository;

    @Transactional(readOnly = true)
    public List<AlcoholResponse> search(String keyword) {
        Set<AlcoholResponse> results = new LinkedHashSet<>(
                alcoholRepository.searchByKeyword(keyword).stream()
                        .map(AlcoholResponse::from)
                        .toList()
        );

        AlcoholCategory matchedCategory = AlcoholCategory.findByNameKo(keyword);
        if (matchedCategory != null) {
            alcoholRepository.findAllByCategory(matchedCategory).stream()
                    .map(AlcoholResponse::from)
                    .forEach(results::add);
        }

        return new ArrayList<>(results);
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
                .orElseThrow(() -> new BusinessException(ErrorCode.ALCOHOL_NOT_FOUND));
    }
}