package com.institutojf.mottainai.client.controller;

import com.institutojf.mottainai.client.dto.request.CustomerLoginRequest;
import com.institutojf.mottainai.client.dto.request.CustomerPasswordRecoveryRequest;
import com.institutojf.mottainai.client.dto.request.CustomerPasswordResetRequest;
import com.institutojf.mottainai.client.dto.request.CustomerResetTokenRequest;
import com.institutojf.mottainai.client.dto.response.CustomerResponse;
import com.institutojf.mottainai.client.dto.response.CustomerTokenResponse;
import com.institutojf.mottainai.client.dto.response.TokenValidationResponse;
import com.institutojf.mottainai.client.service.ClientApiOperations;
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
    public ResponseEntity<Void> requestPasswordRecovery(
            @Valid @RequestBody CustomerPasswordRecoveryRequest request) {
        operations.requestPasswordRecovery(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/password-reset/validate")
    public ResponseEntity<TokenValidationResponse> validatePasswordReset(
            @Valid @RequestBody CustomerResetTokenRequest request) {
        return ResponseEntity.ok(operations.validatePasswordReset(request));
    }

    @PostMapping("/password-reset")
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
