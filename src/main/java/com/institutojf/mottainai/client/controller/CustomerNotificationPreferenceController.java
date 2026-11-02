package com.institutojf.mottainai.client.controller;

import com.institutojf.mottainai.client.notifications.CustomerNotificationPreferences;
import com.institutojf.mottainai.client.notifications.CustomerNotificationPreferences.Preferences;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/** Permite ao cliente controlar seus próprios avisos e o uso temporário da sua localização. */
@RestController
@RequestMapping("/api/v1/client/notification-preferences")
@SecurityRequirement(name="FirebaseBearer")
@Tag(name="Preferências de avisos do cliente")
public class CustomerNotificationPreferenceController {
    private final CustomerNotificationPreferences preferences;

    /** Recebe o serviço de preferências com titularidade transacional. */
    public CustomerNotificationPreferenceController(CustomerNotificationPreferences preferences){this.preferences=preferences;}

    /** Consulta somente o estado vinculado ao Firebase ID token atual. */
    @GetMapping @Operation(summary="Consultar preferência de avisos")
    public Preferences get(@AuthenticationPrincipal Jwt token){return preferences.get(token.getSubject());}

    /** Lista lojas ao redor da posição consentida para registrar a aproximação no aparelho. */
    @GetMapping("/stores") @Operation(summary="Consultar lojas para monitorar aproximação")
    public java.util.List<CustomerNotificationPreferences.NotificationStore> stores(@AuthenticationPrincipal Jwt token){
        return preferences.stores(token.getSubject());
    }

    /** Ativa ou desativa avisos e limita o raio às opções de 5 ou 10 km. */
    @PutMapping @Operation(summary="Atualizar preferência de avisos")
    public Preferences save(@AuthenticationPrincipal Jwt token,@RequestBody PreferenceRequest body){
        return preferences.save(token.getSubject(),body.enabled(),body.radiusKm());
    }

    /** Atualiza uma posição consentida com validade de trinta minutos e rejeita dados antigos. */
    @PutMapping("/location") @Operation(summary="Atualizar posição temporária para avisos próximos")
    public Preferences location(@AuthenticationPrincipal Jwt token,@RequestBody LocationRequest body){
        return preferences.locate(token.getSubject(),body.latitude(),body.longitude(),body.accuracyMeters(),body.observedAt());
    }

    /** Remove a posição sem criar uma nova preferência ou outra identidade. */
    @DeleteMapping("/location") @Operation(summary="Remover posição temporária")
    public ResponseEntity<Void> clear(@AuthenticationPrincipal Jwt token){
        preferences.clearLocation(token.getSubject());return ResponseEntity.noContent().build();
    }

    /** Escolha explícita de recebimento e distância, sem parâmetros de destinatário. */
    public record PreferenceRequest(Boolean enabled,Integer radiusKm){}
    /** Posição obtida do aparelho com instante UTC e precisão informados pelo provedor de localização. */
    public record LocationRequest(Double latitude,Double longitude,Double accuracyMeters,Instant observedAt){}
}
