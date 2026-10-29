package com.institutojf.mottainai.client.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record CustomerLoyaltyTransactionsQuery(
        @Min(0) @Schema(description = "Índice da página, começando em zero", defaultValue = "0", minimum = "0")
        Integer page,
        @Min(1) @Max(100)
        @Schema(description = "Quantidade de transações por página", defaultValue = "20", minimum = "1", maximum = "100")
        Integer size
) {
    public int resolvedPage() {
        return page == null ? 0 : page;
    }

    public int resolvedSize() {
        return size == null ? 20 : size;
    }
}
