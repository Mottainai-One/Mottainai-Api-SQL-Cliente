package com.institutojf.mottainai.client.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateAddressRequest(
        @NotBlank @Pattern(regexp = "\\d{8}") String zipCode,
        @NotBlank @Size(max = 150) String street,
        @NotBlank @Size(max = 10) String number,
        @Size(max = 100) String complement,
        @NotBlank @Size(max = 100) String neighborhood,
        @NotBlank @Size(max = 100) String city,
        @NotBlank @Pattern(regexp = "[A-Z]{2}") String state
) {
}
