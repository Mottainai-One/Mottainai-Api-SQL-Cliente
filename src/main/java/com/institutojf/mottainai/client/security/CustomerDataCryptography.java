package com.institutojf.mottainai.client.security;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import com.institutojf.mottainai.client.config.CustomerDataCryptoProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Encrypts customer private data with the same authenticated envelope used by the operational API. */
@Component
public final class CustomerDataCryptography {

    private static final SecureRandom RANDOM = new SecureRandom();
    private final SecretKey encryptionKey;
    private final short keyVersion;
    private final Map<Short, SecretKey> decryptionKeys;

    public CustomerDataCryptography(
            CustomerDataCryptoProperties properties,
            @Value("${security.staff.data.encryption-key}") String staffEncryptionKey) {
        byte[] encodedKey = Base64.getDecoder().decode(properties.encryptionKey());
        byte[] encodedStaffKey = Base64.getDecoder().decode(staffEncryptionKey);
        if (encodedKey.length != 32 || properties.keyVersion() < 1 || properties.keyVersion() > Short.MAX_VALUE) {
            throw new IllegalStateException("Customer data encryption settings are invalid");
        }
        if (MessageDigest.isEqual(encodedKey, encodedStaffKey)) {
            throw new IllegalStateException("Customer and staff encryption keys must differ");
        }

        this.encryptionKey = new SecretKeySpec(encodedKey, "AES");
        this.keyVersion = (short) properties.keyVersion();
        Map<Short, SecretKey> keys = new HashMap<>();
        keys.put(this.keyVersion, this.encryptionKey);
        if (properties.decryptionKeys() != null && !properties.decryptionKeys().isBlank()) {
            for (String entry : properties.decryptionKeys().split(",")) {
                String[] pair = entry.trim().split(":", 2);
                short version = Short.parseShort(pair[0].trim());
                byte[] oldKey = Base64.getDecoder().decode(pair[1].trim());
                if (MessageDigest.isEqual(oldKey, encodedStaffKey)) {
                    throw new IllegalStateException("Customer decryption keys must differ from staff encryption key");
                }
                keys.put(version, new SecretKeySpec(oldKey, "AES"));
            }
        }
        this.decryptionKeys = Map.copyOf(keys);
    }

    public Envelope encrypt(Integer customerId, String plaintext) {
        byte[] nonce = new byte[12];
        RANDOM.nextBytes(nonce);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, new GCMParameterSpec(128, nonce));
            cipher.updateAAD(aad(customerId));
            return new Envelope(cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8)), nonce, keyVersion);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Could not encrypt customer personal data", exception);
        }
    }

    public String decrypt(Integer customerId, Envelope envelope) {
        if (envelope == null || envelope.nonce() == null || envelope.nonce().length != 12
                || envelope.ciphertext() == null || envelope.ciphertext().length < 17) {
            throw new IllegalStateException("Customer personal data envelope or key version is invalid");
        }
        SecretKey key = decryptionKeys.get(envelope.keyVersion());
        if (key == null) {
            throw new IllegalStateException("No customer data decryption key is configured for this key version");
        }
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, envelope.nonce()));
            cipher.updateAAD(aad(customerId));
            return new String(cipher.doFinal(envelope.ciphertext()), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException exception) {
            throw new IllegalArgumentException("Customer personal data could not be authenticated or decrypted", exception);
        }
    }

    private static byte[] aad(Integer customerId) {
        if (customerId == null || customerId < 1) {
            throw new IllegalArgumentException("A persisted customer ID is required for encryption context");
        }
        return ("mottainai:operational:customer.personal_data:" + customerId).getBytes(StandardCharsets.UTF_8);
    }

    public record Envelope(byte[] ciphertext, byte[] nonce, short keyVersion) {
        public Envelope {
            ciphertext = ciphertext == null ? null : ciphertext.clone();
            nonce = nonce == null ? null : nonce.clone();
        }

        @Override
        public byte[] ciphertext() {
            return ciphertext == null ? null : ciphertext.clone();
        }

        @Override
        public byte[] nonce() {
            return nonce == null ? null : nonce.clone();
        }
    }
}
