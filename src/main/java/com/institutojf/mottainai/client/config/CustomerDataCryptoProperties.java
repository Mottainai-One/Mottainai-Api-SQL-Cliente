package com.institutojf.mottainai.client.config;

import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "security.customer.data")
public record CustomerDataCryptoProperties(
        @NotBlank String encryptionKey,
        @Positive @Max(Short.MAX_VALUE) int keyVersion,
        String decryptionKeys) {

    @AssertTrue(message = "CUSTOMER_DATA_ENCRYPTION_KEY must be Base64 encoding of exactly 32 bytes")
    public boolean isEncryptionKeyValid() {
        return isKey(encryptionKey);
    }

    @AssertTrue(message = "CUSTOMER_DATA_DECRYPTION_KEYS must contain unique version:base64-key entries")
    public boolean isDecryptionKeysValid() {
        if (decryptionKeys == null || decryptionKeys.isBlank()) {
            return true;
        }
        Set<Integer> versions = new HashSet<>();
        versions.add(keyVersion);
        for (String entry : decryptionKeys.split(",")) {
            String[] pair = entry.trim().split(":", 2);
            if (pair.length != 2 || !pair[0].trim().matches("\\d+")) {
                return false;
            }
            try {
                int version = Integer.parseInt(pair[0].trim());
                if (version < 1 || version > Short.MAX_VALUE || !versions.add(version) || !isKey(pair[1].trim())) {
                    return false;
                }
            } catch (NumberFormatException exception) {
                return false;
            }
        }
        return true;
    }

    private static boolean isKey(String encoded) {
        if (encoded == null || encoded.isBlank()) {
            return false;
        }
        try {
            return Base64.getDecoder().decode(encoded).length == 32;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
