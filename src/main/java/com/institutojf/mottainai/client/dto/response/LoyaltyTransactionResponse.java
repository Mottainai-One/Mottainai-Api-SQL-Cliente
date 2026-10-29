package com.institutojf.mottainai.client.dto.response;

import java.time.LocalDateTime;

public record LoyaltyTransactionResponse(
        Integer id,
        Integer saleId,
        LocalDateTime saleDate,
        String transactionType,
        Integer points,
        String description,
        LocalDateTime createdAt
) {
}
