package com.institutojf.mottainai.client.dto.response;

import java.util.List;

/** Mirrors the default Spring Data Page JSON metadata shape used by the operational API. */
public record PageResponse<T>(
        List<T> content,
        PageableMetadata pageable,
        int totalPages,
        long totalElements,
        boolean last,
        int size,
        int number,
        SortMetadata sort,
        boolean first,
        int numberOfElements,
        boolean empty
) {
    public record PageableMetadata(
            SortMetadata sort,
            long offset,
            int pageNumber,
            int pageSize,
            boolean paged,
            boolean unpaged
    ) {
    }

    public record SortMetadata(boolean sorted, boolean unsorted, boolean empty) {
    }
}
