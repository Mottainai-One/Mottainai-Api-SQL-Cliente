package com.institutojf.mottainai.client.dto.response;

import java.time.LocalDateTime;
import java.time.LocalDate;

public record FirebaseCustomerProfileResponse(
        Integer id,
        Short birthYear,
        Boolean active,
        Boolean marketingConsent,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String fullName,
        String phone,
        String email,
        LocalDate birthDate,
        AddressResponse address,
        String avatarUrl
) {
}
