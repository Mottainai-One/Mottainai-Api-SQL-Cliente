package com.institutojf.mottainai.client.controller;

import com.institutojf.mottainai.client.dto.request.CreateFirebaseCustomerProfileRequest;
import com.institutojf.mottainai.client.dto.request.UpdateFirebaseCustomerProfileRequest;
import com.institutojf.mottainai.client.dto.response.CustomerResponse;
import com.institutojf.mottainai.client.service.ClientApiOperations;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/client/auth/profile")
@SecurityRequirement(name = "FirebaseBearer")
@Tag(name = "Perfil do cliente (Firebase)")
public class CustomerProfileController {

    private final ClientApiOperations operations;

    public CustomerProfileController(ClientApiOperations operations) {
        this.operations = operations;
    }

    @GetMapping
    @Operation(summary = "Consultar perfil do cliente autenticado",
            description = "Retorna identificador, ano de nascimento, consentimento de marketing, situação e datas. "
                    + "Por proteção de dados, não retorna nome nem telefone.")
    public CustomerResponse profile(@AuthenticationPrincipal Jwt token) {
        return operations.getFirebaseProfile(token.getSubject());
    }

    @PostMapping
    @Operation(summary = "Criar perfil de cliente vinculado ao Firebase",
            description = "fullName e cpf são obrigatórios. O CPF deve ter 11 dígitos, sem máscara, e não pode "
                    + "estar cadastrado. Telefone, data de nascimento e endereço são opcionais. "
                    + "Se address for enviado, todos os campos obrigatórios dele devem estar preenchidos. "
                    + "O e-mail é obtido do Firebase ID token.")
    public ResponseEntity<CustomerResponse> createProfile(
            @Valid @RequestBody CreateFirebaseCustomerProfileRequest request,
            @AuthenticationPrincipal Jwt token) {
        CustomerResponse response = operations.createFirebaseProfile(
                request, token.getSubject(), token.getClaimAsString("email"));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping
    @Operation(summary = "Atualizar perfil do cliente autenticado",
            description = "fullName e marketingConsent são obrigatórios. Omitir phone ou enviá-lo em branco "
                    + "mantém o telefone atual. Este PUT não é uma atualização parcial livre.")
    public CustomerResponse updateProfile(
            @Valid @RequestBody UpdateFirebaseCustomerProfileRequest request,
            @AuthenticationPrincipal Jwt token) {
        return operations.updateFirebaseProfile(request, token.getSubject());
    }
}
