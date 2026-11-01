package com.institutojf.mottainai.client.dto.response;

import java.time.LocalDateTime;

public record CustomerResponse(
        Integer id,
        Short birthYear,
        Boolean active,
        Boolean marketingConsent,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
