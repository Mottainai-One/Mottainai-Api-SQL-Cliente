package com.institutojf.mottainai.client.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.ModelAttribute;
import com.institutojf.mottainai.client.dto.request.CustomerLoyaltyTransactionsQuery;
import com.institutojf.mottainai.client.dto.response.LoyaltyAccountResponse;
import com.institutojf.mottainai.client.dto.response.LoyaltyTransactionResponse;
import com.institutojf.mottainai.client.dto.response.PageResponse;
import com.institutojf.mottainai.client.service.ClientApiOperations;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/client/loyalty")
@SecurityRequirement(name = "FirebaseBearer")
@Tag(name = "Fidelidade do cliente (Firebase)")
public class ClientLoyaltyController {

    private final ClientApiOperations operations;

    public ClientLoyaltyController(ClientApiOperations operations) {
        this.operations = operations;
    }

    @GetMapping("/balance")
    public LoyaltyAccountResponse balance(@AuthenticationPrincipal Jwt token) {
        return operations.getLoyaltyBalance(token.getSubject());
    }

    @GetMapping("/transactions")
    @Operation(summary = "Consultar histórico de pontos",
            description = "Retorna somente as transações de fidelidade do cliente Firebase autenticado, "
                    + "da mais recente para a mais antiga. from e to são datas inclusivas opcionais. "
                    + "page começa em 0; size varia de 1 a 100, com padrões 0 e 20.")
    public PageResponse<LoyaltyTransactionResponse> transactions(
            @Valid @ModelAttribute CustomerLoyaltyTransactionsQuery query,
            @AuthenticationPrincipal Jwt token) {
        return operations.getLoyaltyTransactions(token.getSubject(), query.resolvedPage(), query.resolvedSize(),
                query.from(), query.to());
    }
}

