package com.institutojf.mottainai.client.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.List;

import com.institutojf.mottainai.client.dto.request.CreateCustomerGeofenceRequest;
import com.institutojf.mottainai.client.dto.response.CustomerGeofenceResponse;
import com.institutojf.mottainai.client.service.ClientApiOperations;
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
@SecurityRequirement(name = "LegacyCustomerBearer")
public class CustomerGeofenceController {

    private final ClientApiOperations operations;

    public CustomerGeofenceController(ClientApiOperations operations) {
        this.operations = operations;
    }

    @GetMapping
    public ResponseEntity<List<CustomerGeofenceResponse>> list(
            @PathVariable Integer customerId,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(operations.listGeofences(customerId, jwt.getSubject()));
    }

    @PostMapping
    public ResponseEntity<CustomerGeofenceResponse> add(
            @PathVariable Integer customerId,
            @Valid @RequestBody CreateCustomerGeofenceRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.status(201)
                .body(operations.addGeofence(customerId, request, jwt.getSubject()));
    }

    @DeleteMapping("/{geofenceId}")
    public ResponseEntity<Void> remove(
            @PathVariable Integer customerId,
            @PathVariable Integer geofenceId,
            @AuthenticationPrincipal Jwt jwt) {
        operations.removeGeofence(customerId, geofenceId, jwt.getSubject());
        return ResponseEntity.noContent().build();
    }
}
