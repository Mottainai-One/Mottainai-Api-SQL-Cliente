package com.institutojf.mottainai.client.security;

import java.util.Base64;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.jwt")
public record JwtProperties(String secret, String issuer, long expirationMinutes) {

    public JwtProperties {
        if (secret == null || issuer == null || issuer.isBlank() || expirationMinutes < 1) {
            throw new IllegalStateException("Legacy customer JWT settings are incomplete");
        }
        try {
            if (Base64.getDecoder().decode(secret).length < 32) {
                throw new IllegalStateException("JWT secret must contain at least 256 bits");
            }
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("JWT secret must be Base64 encoded", exception);
        }
    }
}
