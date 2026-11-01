package com.institutojf.mottainai.client.controller;

import com.institutojf.mottainai.client.dto.request.CreateCustomerGeofenceRequest;
import com.institutojf.mottainai.client.dto.response.CustomerGeofenceResponse;
import com.institutojf.mottainai.client.service.ClientApiOperations;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
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
@RequestMapping("/api/v1/client/geofences")
@SecurityRequirement(name = "FirebaseBearer")
@Tag(name = "Geocercas do cliente (Firebase)")
public class ClientGeofenceController {

    private final ClientApiOperations operations;

    public ClientGeofenceController(ClientApiOperations operations) {
        this.operations = operations;
    }

    @GetMapping
    @Operation(summary = "Listar minhas geocercas",
            description = "O cliente é identificado pelo Firebase ID token; não informe customerId.")
    public ResponseEntity<List<CustomerGeofenceResponse>> list(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(operations.listMyGeofences(jwt.getSubject()));
    }

    @PostMapping
    @Operation(summary = "Criar ou atualizar uma geocerca",
            description = "O proprietário é identificado pelo Firebase ID token; não informe customerId.")
    public ResponseEntity<CustomerGeofenceResponse> add(
            @Valid @RequestBody CreateCustomerGeofenceRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.status(201).body(operations.addMyGeofence(request, jwt.getSubject()));
    }

    @DeleteMapping("/{geofenceId}")
    @Operation(summary = "Desativar uma geocerca do cliente autenticado")
    public ResponseEntity<Void> remove(
            @PathVariable Integer geofenceId,
            @AuthenticationPrincipal Jwt jwt) {
        operations.removeMyGeofence(geofenceId, jwt.getSubject());
        return ResponseEntity.noContent().build();
    }
}
