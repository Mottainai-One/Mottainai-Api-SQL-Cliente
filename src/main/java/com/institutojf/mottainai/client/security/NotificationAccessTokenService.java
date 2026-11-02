package com.institutojf.mottainai.client.security;

import com.institutojf.mottainai.client.dto.response.FirebaseCustomerProfileResponse;
import com.institutojf.mottainai.client.dto.response.NotificationAccessTokenResponse;
import com.institutojf.mottainai.client.service.ClientApiOperations;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Converte a identidade Firebase verificada em acesso temporário à caixa exclusiva do cliente. */
@Service
public class NotificationAccessTokenService {
    public static final long CUSTOMER_RECIPIENT_OFFSET = 1L << 32;
    private final ClientApiOperations operations;
    private final NotificationTokenProperties properties;
    private final JwtEncoder encoder;

    /** Prepara um assinador separado do login legado; a chave deve coincidir com a da API Redis. */
    public NotificationAccessTokenService(ClientApiOperations operations, NotificationTokenProperties properties) {
        this.operations = operations;
        this.properties = properties;
        this.encoder = properties.enabled() ? new NimbusJwtEncoder(new ImmutableSecret<SecurityContext>(
                Base64.getDecoder().decode(properties.secret().trim()))) : null;
    }

    /** Usa somente o perfil ativo vinculado ao Firebase, sem aceitar IDs ou empresa enviados pelo app. */
    public NotificationAccessTokenResponse issue(String firebaseUid, String firebaseEmail) {
        if (!properties.enabled()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Notifications are unavailable");
        }
        FirebaseCustomerProfileResponse profile = operations.getFirebaseProfile(firebaseUid, firebaseEmail);
        if (profile == null || profile.id() == null || profile.id() < 1 || !Boolean.TRUE.equals(profile.active())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Active customer profile required");
        }
        long recipient = recipientId(profile.id());
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("mottainai-client-notifications")
                .audience(List.of("mottainai-redis-notifications"))
                .subject(Long.toString(recipient))
                .issuedAt(now).expiresAt(now.plusSeconds(properties.ttlSeconds()))
                .id(UUID.randomUUID().toString())
                .claim("use", "access").claim("role", "CLIENTE")
                .claim("empresa_id", properties.companyId()).claim("usuario_id", recipient)
                .build();
        String token = encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).type("JWT").build(), claims)).getTokenValue();
        return new NotificationAccessTokenResponse(token, properties.ttlSeconds());
    }

    /** Reserva IDs acima de 2³² para clientes, sem colidir com os IDs INTEGER de funcionários do SQL atual. */
    public static long recipientId(int customerId) {
        if (customerId < 1) throw new IllegalArgumentException("Customer identifier must be positive");
        return CUSTOMER_RECIPIENT_OFFSET + customerId;
    }
}
