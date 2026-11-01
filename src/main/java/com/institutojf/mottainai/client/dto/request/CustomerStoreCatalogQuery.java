package com.institutojf.mottainai.client.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CustomerStoreCatalogQuery(
        @Min(0) Integer page,
        @Min(1) @Max(100) Integer size,
        @Size(max = 100) String query,
        @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal latitude,
        @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal longitude,
        @DecimalMin(value = "0.0", inclusive = false) @DecimalMax("10.0") BigDecimal radiusKm
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
