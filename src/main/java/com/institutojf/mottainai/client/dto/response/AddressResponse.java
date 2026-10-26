package com.institutojf.mottainai.client.dto.response;

public record AddressResponse(
        Integer id,
        String zipCode,
        String street,
        String number,
        String complement,
        String neighborhood,
        String city,
        String state
) {
}
