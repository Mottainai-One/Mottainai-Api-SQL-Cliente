package com.institutojf.mottainai.client.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.List;

import com.institutojf.mottainai.client.dto.request.CreateCustomerGeofenceRequest;
import com.institutojf.mottainai.client.dto.response.CustomerGeofenceResponse;
import com.institutojf.mottainai.client.service.ClientApiOperations;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/customers/{customerId}/geofences")
@SecurityRequirement(name = "FirebaseBearer")
@Tag(name = "Geocercas do cliente (Firebase)")
public class CustomerGeofenceController {

    private final ClientApiOperations operations;

    public CustomerGeofenceController(ClientApiOperations operations) {
        this.operations = operations;
    }

    @GetMapping
    @Operation(summary = "Listar geocercas do cliente autenticado",
            description = "Usa o Firebase ID token. O customerId da URL deve pertencer ao usuário autenticado.")
    public ResponseEntity<List<CustomerGeofenceResponse>> list(
            @PathVariable Integer customerId,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(operations.listGeofences(customerId, jwt.getSubject()));
    }

    @PostMapping
    @Operation(summary = "Criar ou atualizar uma geocerca",
            description = "Usa Firebase ID token. O banco atual permite raios entre 50 e 10.000 metros.")
    public ResponseEntity<CustomerGeofenceResponse> add(
            @PathVariable Integer customerId,
            @Valid @RequestBody CreateCustomerGeofenceRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.status(201)
                .body(operations.addGeofence(customerId, request, jwt.getSubject()));
    }

    @DeleteMapping("/{geofenceId}")
    @Operation(summary = "Desativar uma geocerca", description = "Usa Firebase ID token do proprietário.")
    public ResponseEntity<Void> remove(
            @PathVariable Integer customerId,
            @PathVariable Integer geofenceId,
            @AuthenticationPrincipal Jwt jwt) {
        operations.removeGeofence(customerId, geofenceId, jwt.getSubject());
        return ResponseEntity.noContent().build();
    }
}
