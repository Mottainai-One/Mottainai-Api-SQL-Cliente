package com.institutojf.mottainai.client.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateFirebaseCustomerProfileRequest(
        @NotBlank @Size(max = 150) String fullName,
        @Size(max = 20) String phone,
        @NotNull Boolean marketingConsent
) {
}
