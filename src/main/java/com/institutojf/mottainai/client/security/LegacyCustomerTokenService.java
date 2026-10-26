package com.institutojf.mottainai.client.security;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

@Service
public class LegacyCustomerTokenService {

    private final JwtEncoder jwtEncoder;
    private final JwtProperties jwtProperties;

    public LegacyCustomerTokenService(JwtEncoder legacyJwtEncoder, JwtProperties jwtProperties) {
        this.jwtEncoder = legacyJwtEncoder;
        this.jwtProperties = jwtProperties;
    }

    public String generateAccessToken(String protectedLoginEmail) {
        Instant issuedAt = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(jwtProperties.issuer())
                .subject(protectedLoginEmail)
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plus(jwtProperties.expirationMinutes(), ChronoUnit.MINUTES))
                .claim("roles", List.of("CUSTOMER"))
                .claim("sid", UUID.randomUUID().toString())
                .claim("use", "access")
                .id(UUID.randomUUID().toString())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public long expiresInSeconds() {
        return Math.multiplyExact(jwtProperties.expirationMinutes(), 60L);
    }
}
