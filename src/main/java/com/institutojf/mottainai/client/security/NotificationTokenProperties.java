package com.institutojf.mottainai.client.security;

import java.util.Base64;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuração exclusiva da credencial de leitura da caixa Redis, mantida somente no servidor. */
@ConfigurationProperties(prefix = "security.notifications")
public record NotificationTokenProperties(boolean enabled, String secret, long companyId, long ttlSeconds) {

    /** Valida a configuração ao habilitar a integração, sem impedir os demais serviços quando desabilitada. */
    public NotificationTokenProperties {
        if (enabled) {
            if (companyId < 1 || ttlSeconds < 30 || ttlSeconds > 300) {
                throw new IllegalStateException("Notification company must be positive and TTL must be 30–300 seconds");
            }
            try {
                if (secret == null || Base64.getDecoder().decode(secret.trim()).length < 32) {
                    throw new IllegalStateException("Notification signing key must contain at least 256 bits");
                }
            } catch (IllegalArgumentException exception) {
                throw new IllegalStateException("Notification signing key must be Base64 encoded");
            }
        }
    }

    /** Evita que a representação textual da configuração revele a chave de assinatura. */
    @Override public String toString() {
        return "NotificationTokenProperties[enabled=" + enabled + ", secret=[REDACTED], companyId="
                + companyId + ", ttlSeconds=" + ttlSeconds + "]";
    }
}
