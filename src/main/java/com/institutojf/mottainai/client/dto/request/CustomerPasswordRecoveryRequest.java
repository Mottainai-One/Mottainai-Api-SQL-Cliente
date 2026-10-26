package com.institutojf.mottainai.client.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CustomerPasswordRecoveryRequest(
        @NotBlank @Email String email
) {
}
