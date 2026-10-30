package com.institutojf.mottainai.client.dto.response;

import java.math.BigDecimal;

public record CustomerCatalogStoreResponse(
        Integer id,
        String name,
        AddressResponse address,
        BigDecimal latitude,
        BigDecimal longitude
) {
}
