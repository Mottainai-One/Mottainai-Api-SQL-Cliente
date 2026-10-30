package com.institutojf.mottainai.client.dto.response;

public record CustomerTokenResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {
}
