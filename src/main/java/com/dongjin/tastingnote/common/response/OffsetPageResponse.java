package com.dongjin.tastingnote.common.response;

import java.util.List;

public record OffsetPageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {}