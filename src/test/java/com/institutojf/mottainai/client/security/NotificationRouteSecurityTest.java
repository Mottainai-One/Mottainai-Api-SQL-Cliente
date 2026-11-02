package com.institutojf.mottainai.client.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.institutojf.mottainai.client.controller.ClientNotificationController;
import com.institutojf.mottainai.client.controller.CustomerNotificationPreferenceController;
import com.institutojf.mottainai.client.notifications.CustomerNotificationPreferences;
import com.institutojf.mottainai.client.dto.response.FirebaseCustomerProfileResponse;
import com.institutojf.mottainai.client.service.ClientApiOperations;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/** Exercita a cadeia real de segurança HTTP; simula somente o Firebase externo e a consulta SQL do perfil. */
@SpringJUnitWebConfig({SecurityConfig.class, ClientNotificationController.class,
        CustomerNotificationPreferenceController.class,
        NotificationAccessTokenService.class, NotificationRouteSecurityTest.Dependencies.class})
@TestPropertySource(properties = {
        "security.firebase.project-id=notification-tests",
        "security.jwt.secret=MTIzNDU2Nzg5MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTI=",
        "security.jwt.issuer=test-legacy", "security.jwt.expiration-minutes=60",
        "security.customer.data.encryption-key=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
        "security.customer.data.key-version=1",
        "security.notifications.enabled=true",
        "security.notifications.secret=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
        "security.notifications.company-id=7", "security.notifications.ttl-seconds=300"
})
class NotificationRouteSecurityTest {
    private static final String PATH = "/api/v1/client/notifications/access-token";
    @Autowired private WebApplicationContext context;
    @Autowired private ClientApiOperations operations;
    @Autowired private CustomerNotificationPreferences preferences;
    @MockitoBean(name = "firebaseJwtDecoder") private JwtDecoder firebase;
    private MockMvc http;

    /** Recria o cliente HTTP com os filtros da aplicação e prepara uma identidade Firebase autenticada. */
    @BeforeEach void setup() {
        reset(operations,preferences);
        when(firebase.decode(anyString())).thenThrow(new BadJwtException("Invalid Firebase token"));
        doReturn(Jwt.withTokenValue("valid-firebase")
                .header("alg", "RS256").subject("firebase-a").claim("email", "a@example.com").build())
                .when(firebase).decode("valid-firebase");
        when(operations.getFirebaseProfile("firebase-a", "a@example.com")).thenReturn(
                new FirebaseCustomerProfileResponse(12, null, true, false, null, null,
                        "Customer", null, "a@example.com", null, null, null));
        http = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(context.getBean("springSecurityFilterChain", Filter.class)).build();
    }

    /** Uma requisição anônima não pode obter uma credencial nem consultar perfis. */
    @Test void anonymousRequestIsUnauthorized() throws Exception {
        http.perform(post(PATH)).andExpect(status().isUnauthorized());
        verifyNoInteractions(operations);
    }

    /** Tokens que não foram validados pelo fluxo Firebase são rejeitados antes da consulta SQL. */
    @Test void invalidOrLegacyBearerIsUnauthorized() throws Exception {
        http.perform(post(PATH).header("Authorization", "Bearer legacy-or-invalid"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(operations);
    }

    /** A rota responde com o contrato usado pelo Android e impede cache da credencial. */
    @Test void authenticatedCustomerGetsNonCacheableAccess() throws Exception {
        String body = http.perform(post(PATH).header("Authorization", "Bearer valid-firebase"))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(header().string("Pragma", "no-cache")).andReturn().getResponse().getContentAsString();
        var json = new ObjectMapper().readTree(body);
        assertEquals(300, json.get("expiresIn").asLong());
        assertEquals(3, json.get("accessToken").asText().split("\\.").length);
        verify(operations).getFirebaseProfile("firebase-a", "a@example.com");
    }

    /** Um parâmetro enviado pelo aparelho não escolhe o cliente ou a empresa do token. */
    @Test void requestCannotSelectAnotherRecipient() throws Exception {
        String body = http.perform(post(PATH).header("Authorization", "Bearer valid-firebase")
                        .queryParam("customerId", "99").queryParam("empresa_id", "99"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String token = new ObjectMapper().readTree(body).get("accessToken").asText();
        var claims = new ObjectMapper().readTree(java.util.Base64.getUrlDecoder().decode(token.split("\\.")[1]));
        assertEquals("4294967308", claims.get("sub").asText());
        assertEquals(7, claims.get("empresa_id").asLong());
        verify(operations).getFirebaseProfile("firebase-a", "a@example.com");
    }

    /** A ausência de perfil retorna o erro de cadastro, sem produzir uma resposta de token. */
    @Test void missingProfileIsNotFound() throws Exception {
        when(operations.getFirebaseProfile("firebase-a", "a@example.com"))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found"));
        http.perform(post(PATH).header("Authorization", "Bearer valid-firebase")).andExpect(status().isNotFound());
    }

    /** Um perfil inativo não recebe acesso à caixa. */
    @Test void inactiveProfileIsForbidden() throws Exception {
        when(operations.getFirebaseProfile("firebase-a", "a@example.com")).thenReturn(
                new FirebaseCustomerProfileResponse(12, null, false, false, null, null,
                        null, null, null, null, null, null));
        http.perform(post(PATH).header("Authorization", "Bearer valid-firebase")).andExpect(status().isForbidden());
    }

    /** A nova credencial não substitui o token Firebase para acessar a própria rota de emissão. */
    @Test void redisTokenCannotBeExchangedAsFirebase() throws Exception {
        String body = http.perform(post(PATH).header("Authorization", "Bearer valid-firebase"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String redis = new ObjectMapper().readTree(body).get("accessToken").asText();
        http.perform(post(PATH).header("Authorization", "Bearer " + redis)).andExpect(status().isUnauthorized());
    }

    /** Nenhuma rota de preferência aceita uma requisição anônima ou um token legado. */
    @Test void allPreferenceRoutesRequireFirebase()throws Exception{
        String path="/api/v1/client/notification-preferences";
        for(String token:new String[]{"","Bearer legacy-or-invalid"}){
            http.perform(get(path).header("Authorization",token)).andExpect(status().isUnauthorized());
            http.perform(put(path).header("Authorization",token).contentType("application/json").content("{\"enabled\":true,\"radiusKm\":5}"))
                .andExpect(status().isUnauthorized());
            http.perform(get(path+"/stores").header("Authorization",token)).andExpect(status().isUnauthorized());
            http.perform(put(path+"/location").header("Authorization",token).contentType("application/json").content("{}"))
                .andExpect(status().isUnauthorized());
            http.perform(delete(path+"/location").header("Authorization",token)).andExpect(status().isUnauthorized());
        }
        verifyNoInteractions(preferences);
    }

    /** A identidade usada pelo serviço vem do token, mesmo que o corpo tente escolher outro cliente. */
    @Test void preferenceUpdateUsesOnlyAuthenticatedIdentity()throws Exception{
        when(preferences.save("firebase-a",true,5)).thenReturn(new CustomerNotificationPreferences.Preferences(true,5,null,null));
        http.perform(put("/api/v1/client/notification-preferences").header("Authorization","Bearer valid-firebase")
            .contentType("application/json").content("{\"enabled\":true,\"radiusKm\":5,\"customerId\":999}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.enabled").value(true)).andExpect(jsonPath("$.radiusKm").value(5));
        verify(preferences).save("firebase-a",true,5);
    }

    /** Consulta, geocercas e remoção de posição preservam a titularidade e não aceitam IDs da URL. */
    @Test void authenticatedPreferenceReadsAndRemovalUseTokenSubject()throws Exception{
        when(preferences.get("firebase-a")).thenReturn(new CustomerNotificationPreferences.Preferences(false,5,null,null));
        when(preferences.stores("firebase-a")).thenReturn(java.util.List.of());
        String path="/api/v1/client/notification-preferences";
        http.perform(get(path).header("Authorization","Bearer valid-firebase")).andExpect(status().isOk());
        http.perform(get(path+"/stores").header("Authorization","Bearer valid-firebase")).andExpect(status().isOk());
        http.perform(delete(path+"/location").header("Authorization","Bearer valid-firebase")).andExpect(status().isNoContent());
        verify(preferences).get("firebase-a");verify(preferences).stores("firebase-a");verify(preferences).clearLocation("firebase-a");
    }

    /** Fornece somente os componentes necessários ao teste web, sem inicializar banco ou e-mail. */
    @Configuration @EnableWebMvc
    static class Dependencies {
        /** Substitui o repositório de dados para testar o vínculo sem usar dados de uma pessoa real. */
        @Bean ClientApiOperations operations() { return mock(ClientApiOperations.class); }
        /** Simula a persistência para conferir apenas filtros HTTP e titularidade das novas rotas. */
        @Bean CustomerNotificationPreferences preferences(){return mock(CustomerNotificationPreferences.class);}
    }
}
