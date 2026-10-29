package com.institutojf.mottainai.client.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CustomerCatalogPromotionResponse(
        Integer id,
        String name,
        String description,
        String promotionType,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        CustomerCatalogStoreResponse store,
        BigDecimal distanceKm,
        BigDecimal discountPercentage,
        List<Item> items
) {
    public record Item(
            Integer id,
            Integer productId,
            String name,
            BigDecimal originalPrice,
            BigDecimal promotionalPrice,
            BigDecimal quantityAvailable
    ) {
    }
}
