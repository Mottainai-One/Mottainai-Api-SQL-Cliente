package com.institutojf.mottainai.client.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;

public record CreateCustomerGeofenceRequest(
        @NotNull Integer storeId,
        @NotNull @Min(50) @Max(10000)
        @Schema(description = "Raio de busca do cliente: de 50 a 10.000 metros",
                minimum = "50", maximum = "10000", example = "5000")
        Integer radiusMeters
) {
}
