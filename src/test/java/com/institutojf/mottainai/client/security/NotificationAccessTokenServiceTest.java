package com.institutojf.mottainai.client.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.institutojf.mottainai.client.dto.response.FirebaseCustomerProfileResponse;
import com.institutojf.mottainai.client.service.ClientApiOperations;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.web.server.ResponseStatusException;

/** Verifica assinatura, duração, isolamento das caixas e falhas sem acessar banco ou Firebase reais. */
class NotificationAccessTokenServiceTest {
    private static final String KEY = Base64.getEncoder().encodeToString(new byte[32]);
    private final ClientApiOperations operations = mock(ClientApiOperations.class);
    private final NotificationTokenProperties properties = new NotificationTokenProperties(true, KEY, 7, 300);
    private final NotificationAccessTokenService service = new NotificationAccessTokenService(operations, properties);

    /** Confere o mesmo algoritmo e os campos exigidos pelo AuthService da Redis API. */
    @Test void signsShortTokenForTheAuthenticatedCustomerWithoutPersonalData() {
        when(operations.getFirebaseProfile("firebase-a", "a@example.com")).thenReturn(profile(12, true));
        Instant before = Instant.now();
        var response = service.issue("firebase-a", "a@example.com");
        Jwt token = decoder(KEY).decode(response.accessToken());
        assertEquals("HS256", token.getHeaders().get("alg"));
        assertEquals("4294967308", token.getSubject());
        assertEquals(4294967308L, ((Number) token.getClaim("usuario_id")).longValue());
        assertEquals(7L, ((Number) token.getClaim("empresa_id")).longValue());
        assertEquals("CLIENTE", token.getClaimAsString("role"));
        assertEquals("access", token.getClaimAsString("use"));
        assertEquals("mottainai-client-notifications", token.getClaimAsString("iss"));
        assertEquals(java.util.List.of("mottainai-redis-notifications"), token.getAudience());
        assertEquals(300, response.expiresIn());
        assertEquals(300, token.getExpiresAt().getEpochSecond() - token.getIssuedAt().getEpochSecond());
        assertTrue(token.getExpiresAt().isAfter(before));
        for (String field : java.util.List.of("email", "cpf", "phone", "firebaseUid", "roles")) {
            assertFalse(token.hasClaim(field));
        }
        assertFalse(response.toString().contains(response.accessToken()));
    }

    /** Garante que duas contas nunca recebam a mesma caixa e que novas emissões tenham identificadores próprios. */
    @Test void separatesAccountsAndGeneratesFreshTokens() {
        when(operations.getFirebaseProfile("a", null)).thenReturn(profile(1, true));
        when(operations.getFirebaseProfile("b", null)).thenReturn(profile(2, true));
        Jwt first = decoder(KEY).decode(service.issue("a", null).accessToken());
        Jwt second = decoder(KEY).decode(service.issue("b", null).accessToken());
        Jwt renewed = decoder(KEY).decode(service.issue("a", null).accessToken());
        assertNotEquals(first.getSubject(), second.getSubject());
        assertEquals(first.getSubject(), renewed.getSubject());
        assertNotEquals(first.getId(), renewed.getId());
        assertTrue(Long.parseLong(first.getSubject()) > Integer.MAX_VALUE);
    }

    /** Recusa perfis inativos ou com identidade inválida antes de assinar qualquer acesso. */
    @Test void refusesInactiveAndInvalidProfiles() {
        for (FirebaseCustomerProfileResponse profile : java.util.List.of(profile(1, false), profile(0, true))) {
            when(operations.getFirebaseProfile("a", null)).thenReturn(profile);
            var error = assertThrows(ResponseStatusException.class, () -> service.issue("a", null));
            assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        }
    }

    /** Mantém o erro de perfil ausente para o app concluir o cadastro antes de receber avisos. */
    @Test void preservesMissingProfileFailure() {
        when(operations.getFirebaseProfile("a", null)).thenThrow(
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found"));
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(ResponseStatusException.class,
                () -> service.issue("a", null)).getStatusCode());
    }

    /** A integração desabilitada não consulta o banco nem produz uma credencial. */
    @Test void disabledIntegrationDoesNotReadProfiles() {
        var disabled = new NotificationAccessTokenService(operations,
                new NotificationTokenProperties(false, "", 0, 300));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, assertThrows(ResponseStatusException.class,
                () -> disabled.issue("a", null)).getStatusCode());
        verifyNoInteractions(operations);
    }

    /** A assinatura impede que outro segredo valide uma credencial emitida para esta integração. */
    @Test void rejectsWrongSignature() {
        when(operations.getFirebaseProfile("a", null)).thenReturn(profile(1, true));
        String access = service.issue("a", null).accessToken();
        String other = Base64.getEncoder().encodeToString("12345678901234567890123456789012".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertThrows(JwtException.class, () -> decoder(other).decode(access));
    }

    /** A configuração ativa exige chave forte, empresa definida e duração curta. */
    @Test void validatesEnabledConfigurationWithoutLeakingKey() {
        assertThrows(IllegalStateException.class, () -> new NotificationTokenProperties(true, "invalid", 7, 300));
        assertThrows(IllegalStateException.class, () -> new NotificationTokenProperties(true, "", 7, 300));
        assertThrows(IllegalStateException.class, () -> new NotificationTokenProperties(true, KEY, 0, 300));
        assertThrows(IllegalStateException.class, () -> new NotificationTokenProperties(true, KEY, 7, 3600));
        assertThrows(IllegalStateException.class, () -> new NotificationTokenProperties(true, KEY, 7, 1));
        assertFalse(properties.toString().contains(KEY));
    }

    /** Confere os extremos INTEGER sem sobreposição com funcionários ou perda de precisão. */
    @Test void mapsOnlyPositiveSqlCustomerIdsIntoTheReservedRange() {
        assertEquals(4294967297L, NotificationAccessTokenService.recipientId(1));
        assertEquals(6442450943L, NotificationAccessTokenService.recipientId(Integer.MAX_VALUE));
        assertThrows(IllegalArgumentException.class, () -> NotificationAccessTokenService.recipientId(0));
        assertThrows(IllegalArgumentException.class, () -> NotificationAccessTokenService.recipientId(-1));
    }

    /** Monta um perfil mínimo vinculado ao identificador SQL validado. */
    private static FirebaseCustomerProfileResponse profile(int id, boolean active) {
        return new FirebaseCustomerProfileResponse(id, null, active, false, null, null,
                "Customer", "11999999999", "a@example.com", null, null, null);
    }

    /** Verifica a credencial com a chave compartilhada e HS256, sem conexão de rede. */
    private static NimbusJwtDecoder decoder(String key) {
        return NimbusJwtDecoder.withSecretKey(new SecretKeySpec(Base64.getDecoder().decode(key), "HmacSHA256"))
                .macAlgorithm(MacAlgorithm.HS256).build();
    }
}
