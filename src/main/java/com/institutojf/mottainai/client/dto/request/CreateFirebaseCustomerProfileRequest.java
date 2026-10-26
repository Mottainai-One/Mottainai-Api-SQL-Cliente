package com.institutojf.mottainai.client.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CreateFirebaseCustomerProfileRequest(
        @NotBlank @Size(max = 150) String fullName,
        @NotBlank @Pattern(regexp = "\\d{11}") String cpf,
        @Size(max = 20) String phone,
        LocalDate birthDate,
        Boolean marketingConsent,
        @Valid CreateAddressRequest address
) {
}
