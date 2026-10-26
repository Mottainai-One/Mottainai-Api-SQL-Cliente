package com.institutojf.mottainai.client.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateCustomerGeofenceRequest(
        @NotNull Integer storeId,
        @NotNull @Min(50) @Max(10000) Integer radiusMeters
) {
}
