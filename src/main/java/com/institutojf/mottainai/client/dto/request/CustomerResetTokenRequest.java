package com.institutojf.mottainai.client.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CustomerResetTokenRequest(
        @NotBlank String token
) {
}
