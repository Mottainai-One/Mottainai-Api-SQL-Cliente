package com.institutojf.mottainai.client.controller;

import com.institutojf.mottainai.client.dto.response.NotificationAccessTokenResponse;
import com.institutojf.mottainai.client.security.NotificationAccessTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Expõe a troca autenticada de credencial sem permitir que o aplicativo escolha outro destinatário. */
@RestController
@RequestMapping("/api/v1/client/notifications")
@SecurityRequirement(name = "FirebaseBearer")
@Tag(name = "Avisos do cliente (Firebase)")
public class ClientNotificationController {
    private final NotificationAccessTokenService tokens;

    /** Recebe o emissor de credenciais de notificações. */
    public ClientNotificationController(NotificationAccessTokenService tokens) {
        this.tokens = tokens;
    }

    /** Retorna uma credencial temporária sem cache para consultar e marcar os próprios avisos na Redis API. */
    @PostMapping("/access-token")
    @Operation(summary = "Obter acesso temporário aos avisos do próprio cliente",
            description = "Sem corpo ou parâmetros de identidade. Requer Firebase ID token e perfil ativo. "
                    + "Retorna 503 quando a integração está desabilitada. Não cria nem envia notificações.")
    public ResponseEntity<NotificationAccessTokenResponse> accessToken(@AuthenticationPrincipal Jwt token) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).header("Pragma", "no-cache")
                .body(tokens.issue(token.getSubject(), token.getClaimAsString("email")));
    }
}
