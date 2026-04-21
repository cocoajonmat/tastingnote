package com.dongjin.tastingnote.alcohol.service;

import com.dongjin.tastingnote.alcohol.dto.AlcoholResponse;
import com.dongjin.tastingnote.alcohol.entity.AlcoholCategory;
import com.dongjin.tastingnote.alcohol.repository.AlcoholRepository;
import com.dongjin.tastingnote.common.cursor.CursorUtils;
import com.dongjin.tastingnote.common.exception.BusinessException;
import com.dongjin.tastingnote.common.exception.ErrorCode;
import com.dongjin.tastingnote.common.response.CursorPageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AlcoholService {

    private final AlcoholRepository alcoholRepository;

    @Transactional(readOnly = true)
    public CursorPageResponse<AlcoholResponse> search(String keyword, String cursor, int size) {
        String trimmed = keyword.trim();
        long cursorId = CursorUtils.parseLongId(cursor);
        List<AlcoholResponse> fetched = alcoholRepository
                .searchByKeywordWithCursor(trimmed, cursorId, PageRequest.of(0, size + 1))
                .stream()
                .map(AlcoholResponse::from)
                .toList();
        return buildCursorResponse(fetched, size);
    }

    @Transactional(readOnly = true)
    public CursorPageResponse<AlcoholResponse> getByCategory(AlcoholCategory category, String cursor, int size) {
        long cursorId = CursorUtils.parseLongId(cursor);
        List<AlcoholResponse> fetched = alcoholRepository
                .findByCategoryWithCursor(category, cursorId, PageRequest.of(0, size + 1))
                .stream()
                .map(AlcoholResponse::from)
                .toList();
        return buildCursorResponse(fetched, size);
    }

    @Transactional(readOnly = true)
    public AlcoholResponse getById(Long id) {
        return alcoholRepository.findById(id)
                .map(AlcoholResponse::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.ALCOHOL_NOT_FOUND));
    }

    private CursorPageResponse<AlcoholResponse> buildCursorResponse(List<AlcoholResponse> fetched, int size) {
        boolean hasNext = fetched.size() > size;
        List<AlcoholResponse> content = hasNext ? fetched.subList(0, size) : fetched;
        String nextCursor = hasNext
                ? CursorUtils.encode(Map.of("id", String.valueOf(content.get(content.size() - 1).getId())))
                : null;
        return new CursorPageResponse<>(content, nextCursor, hasNext);
    }
}