package com.institutojf.mottainai.client.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record CustomerPromotionCatalogQuery(
        @Min(0) Integer page,
        @Min(1) @Max(100) Integer size,
        Integer storeId,
        @Size(max = 100) String query
) {
    public int resolvedPage() {
        return page == null ? 0 : page;
    }

    public int resolvedSize() {
        return size == null ? 20 : size;
    }
}
