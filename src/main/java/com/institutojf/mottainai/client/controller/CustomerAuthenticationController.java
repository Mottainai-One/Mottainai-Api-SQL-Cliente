package com.institutojf.mottainai.client.controller;

import com.institutojf.mottainai.client.dto.request.CustomerLoginRequest;
import com.institutojf.mottainai.client.dto.request.CustomerPasswordRecoveryRequest;
import com.institutojf.mottainai.client.dto.request.CustomerPasswordResetRequest;
import com.institutojf.mottainai.client.dto.request.CustomerResetTokenRequest;
import com.institutojf.mottainai.client.dto.response.CustomerResponse;
import com.institutojf.mottainai.client.dto.response.CustomerTokenResponse;
import com.institutojf.mottainai.client.dto.response.TokenValidationResponse;
import com.institutojf.mottainai.client.service.ClientApiOperations;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/customers/auth")
@Tag(name = "Autenticação legada de clientes")
public class CustomerAuthenticationController {

    private final ClientApiOperations operations;

    public CustomerAuthenticationController(ClientApiOperations operations) {
        this.operations = operations;
    }

    @PostMapping("/login")
    public ResponseEntity<CustomerTokenResponse> login(
            @Valid @RequestBody CustomerLoginRequest request) {
        return ResponseEntity.ok(operations.login(request));
    }

    @PostMapping("/password-recovery")
    @Operation(summary = "Solicitar recuperação de senha",
            description = "Retorna 204 sem revelar se a conta existe. A resposta confirma o processamento da "
                    + "solicitação, mas não garante que um e-mail foi enviado; SMTP e URL de recuperação precisam "
                    + "estar configurados.")
    public ResponseEntity<Void> requestPasswordRecovery(
            @Valid @RequestBody CustomerPasswordRecoveryRequest request) {
        operations.requestPasswordRecovery(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/password-reset/validate")
    @Operation(summary = "Validar token de recuperação",
            description = "Retorna 400 quando o token tem formato inválido. Com formato válido, retorna "
                    + "{valid:false} se o token estiver expirado, já usado ou não corresponder ao registro.")
    public ResponseEntity<TokenValidationResponse> validatePasswordReset(
            @Valid @RequestBody CustomerResetTokenRequest request) {
        return ResponseEntity.ok(operations.validatePasswordReset(request));
    }

    @PostMapping("/password-reset")
    @Operation(summary = "Redefinir senha",
            description = "A nova senha deve ter de 8 a 100 caracteres, sem espaços, com maiúscula, minúscula, "
                    + "número e caractere especial.")
    public ResponseEntity<Void> resetPassword(
            @Valid @RequestBody CustomerPasswordResetRequest request) {
        operations.resetPassword(request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/profile")
    @SecurityRequirement(name = "LegacyCustomerBearer")
    public ResponseEntity<CustomerResponse> profile(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(operations.getLegacyProfile(jwt.getSubject()));
    }
}
