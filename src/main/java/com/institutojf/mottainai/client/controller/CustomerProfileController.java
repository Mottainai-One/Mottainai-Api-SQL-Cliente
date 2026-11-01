package com.institutojf.mottainai.client.controller;

import com.institutojf.mottainai.client.dto.request.CreateFirebaseCustomerProfileRequest;
import com.institutojf.mottainai.client.dto.request.UpdateFirebaseCustomerProfileRequest;
import com.institutojf.mottainai.client.dto.response.FirebaseCustomerProfileResponse;
import com.institutojf.mottainai.client.dto.response.CustomerAvatarResponse;
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
            description = "Retorna os dados do perfil do cliente autenticado. O e-mail vem da identidade Firebase; "
                    + "os demais dados pessoais são descriptografados do envelope privado do próprio cliente.")
    public FirebaseCustomerProfileResponse profile(@AuthenticationPrincipal Jwt token) {
        return operations.getFirebaseProfile(token.getSubject(), token.getClaimAsString("email"));
    }

    @GetMapping("/avatar")
    @Operation(summary = "Consultar somente a URL da foto do cliente autenticado",
            description = "Retorna avatarUrl do perfil Firebase. Se nenhuma foto estiver cadastrada, avatarUrl será null.")
    public CustomerAvatarResponse profileAvatar(@AuthenticationPrincipal Jwt token) {
        return new CustomerAvatarResponse(operations.getFirebaseProfileAvatarUrl(token.getSubject()));
    }

    @PostMapping
    @Operation(summary = "Criar perfil de cliente vinculado ao Firebase",
            description = "fullName e cpf são obrigatórios. O CPF deve ter 11 dígitos, sem máscara, e não pode "
                    + "estar cadastrado. Telefone, data de nascimento e endereço são opcionais. "
                    + "Se address for enviado, todos os campos obrigatórios dele devem estar preenchidos. "
                    + "O e-mail é obtido do Firebase ID token. avatarUrl, quando informado, deve ser uma URL HTTPS "
                    + "de res.cloudinary.com.")
    public ResponseEntity<FirebaseCustomerProfileResponse> createProfile(
            @Valid @RequestBody CreateFirebaseCustomerProfileRequest request,
            @AuthenticationPrincipal Jwt token) {
        FirebaseCustomerProfileResponse response = operations.createFirebaseProfile(
                request, token.getSubject(), token.getClaimAsString("email"));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping
    @Operation(summary = "Atualizar perfil do cliente autenticado",
            description = "fullName e marketingConsent são obrigatórios. Omitir phone ou enviá-lo em branco "
                    + "mantém o telefone atual. avatarUrl aceita URL segura do Cloudinary; envie vazio para removê-la. "
                    + "Este PUT não é uma atualização parcial livre.")
    public FirebaseCustomerProfileResponse updateProfile(
            @Valid @RequestBody UpdateFirebaseCustomerProfileRequest request,
            @AuthenticationPrincipal Jwt token) {
        return operations.updateFirebaseProfile(request, token.getSubject(), token.getClaimAsString("email"));
    }
}

