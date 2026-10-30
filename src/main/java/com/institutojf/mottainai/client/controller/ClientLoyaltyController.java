package com.institutojf.mottainai.client.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import com.institutojf.mottainai.client.dto.response.LoyaltyAccountResponse;
import com.institutojf.mottainai.client.service.ClientApiOperations;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/client/loyalty")
@SecurityRequirement(name = "FirebaseBearer")
public class ClientLoyaltyController {

    private final ClientApiOperations operations;

    public ClientLoyaltyController(ClientApiOperations operations) {
        this.operations = operations;
    }

    @GetMapping("/balance")
    public LoyaltyAccountResponse balance(@AuthenticationPrincipal Jwt token) {
        return operations.getLoyaltyBalance(token.getSubject());
    }
}
