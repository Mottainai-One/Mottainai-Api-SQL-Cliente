package com.institutojf.mottainai.client.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import io.swagger.v3.oas.annotations.media.Schema;

public record CustomerPromotionCatalogQuery(
        @Min(0) @Schema(description = "Índice da página, começando em zero", defaultValue = "0", minimum = "0")
        Integer page,
        @Min(1) @Max(100)
        @Schema(description = "Quantidade de itens por página", defaultValue = "20", minimum = "1", maximum = "100")
        Integer size,
        Integer storeId,
        @Size(max = 100) String query,
        @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal latitude,
        @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal longitude,
        @DecimalMin(value = "0.0", inclusive = false) @DecimalMax("20.0") BigDecimal radiusKm
) {
    @AssertTrue(message = "Latitude, longitude, and radiusKm must be provided together")
    public boolean isGeographicSearchComplete() {
        int suppliedValues = (latitude == null ? 0 : 1)
                + (longitude == null ? 0 : 1)
                + (radiusKm == null ? 0 : 1);
        return suppliedValues == 0 || suppliedValues == 3;
    }

    public int resolvedPage() {
        return page == null ? 0 : page;
    }

    public int resolvedSize() {
        return size == null ? 20 : size;
    }
}
