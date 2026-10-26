package com.institutojf.mottainai.client.dto.response;

import java.time.LocalDateTime;

public record CustomerGeofenceResponse(
        Integer id,
        Integer customerId,
        Integer storeId,
        String storeName,
        Integer radiusMeters,
        Boolean active,
        LocalDateTime createdAt
) {
}
