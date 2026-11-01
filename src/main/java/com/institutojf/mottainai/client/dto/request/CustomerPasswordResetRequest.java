package com.institutojf.mottainai.client.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CustomerPasswordResetRequest(
        @NotBlank String token,
        @NotBlank @Size(min = 8, max = 100)
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9\\s])\\S{8,100}$",
                message = "Password must have upper and lower case, a digit, a special character, and no spaces")
        String newPassword
) {
}
