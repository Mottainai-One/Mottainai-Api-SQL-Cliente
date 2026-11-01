package com.institutojf.mottainai.client.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CreateFirebaseCustomerProfileRequest(
        @NotBlank @Size(max = 150)
        @Schema(description = "Nome completo do cliente", requiredMode = Schema.RequiredMode.REQUIRED)
        String fullName,
        @NotBlank @Pattern(regexp = "\\d{11}")
        @Schema(description = "CPF com 11 dígitos, sem máscara e ainda não cadastrado",
                example = "12345678909", requiredMode = Schema.RequiredMode.REQUIRED)
        String cpf,
        @Size(max = 20) String phone,
        LocalDate birthDate,
        @NotNull Boolean marketingConsent,
        @Valid CreateAddressRequest address,
        @Size(max = 2048) @Schema(description = "URL segura da foto de perfil no Cloudinary") String avatarUrl
) {
}
