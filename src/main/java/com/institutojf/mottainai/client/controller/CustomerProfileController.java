package com.institutojf.mottainai.client.controller;

import com.institutojf.mottainai.client.dto.request.CreateFirebaseCustomerProfileRequest;
import com.institutojf.mottainai.client.dto.request.UpdateFirebaseCustomerProfileRequest;
import com.institutojf.mottainai.client.dto.response.CustomerResponse;
import com.institutojf.mottainai.client.service.ClientApiOperations;
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
public class CustomerProfileController {

    private final ClientApiOperations operations;

    public CustomerProfileController(ClientApiOperations operations) {
        this.operations = operations;
    }

    @GetMapping
    public CustomerResponse profile(@AuthenticationPrincipal Jwt token) {
        return operations.getFirebaseProfile(token.getSubject());
    }

    @PostMapping
    public ResponseEntity<CustomerResponse> createProfile(
            @Valid @RequestBody CreateFirebaseCustomerProfileRequest request,
            @AuthenticationPrincipal Jwt token) {
        CustomerResponse response = operations.createFirebaseProfile(
                request, token.getSubject(), token.getClaimAsString("email"));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping
    public CustomerResponse updateProfile(
            @Valid @RequestBody UpdateFirebaseCustomerProfileRequest request,
            @AuthenticationPrincipal Jwt token) {
        return operations.updateFirebaseProfile(request, token.getSubject());
    }
}
