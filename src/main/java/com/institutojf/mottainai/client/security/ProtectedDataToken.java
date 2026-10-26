package com.institutojf.mottainai.client.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.StringJoiner;
import java.util.function.UnaryOperator;

/** Produces the deterministic tokens required by the protected customer schema. */
public final class ProtectedDataToken {

    private ProtectedDataToken() {
    }

    public static String customerName(String value) {
        return token("CUS", value, ProtectedDataToken::trimSpaces);
    }

    public static String cpf(String value) {
        return token("CPF", value, text -> text.replaceAll("\\D", ""));
    }

    public static String email(String value) {
        return token("EMAIL", value, text -> trimSpaces(text).toLowerCase(Locale.ROOT));
    }

    public static String phone(String value) {
        return token("PHONE", value, text -> text.replaceAll("\\D", ""));
    }

    public static String externalAuthUid(String value) {
        return token("AUTH", value, ProtectedDataToken::trimSpaces);
    }

    public static String address(String zipCode, String street, String number, String complement,
            String neighborhood, String city, String state) {
        StringJoiner value = new StringJoiner("|");
        for (String part : new String[] { zipCode, street, number, complement, neighborhood, city, state }) {
            if (part != null) {
                value.add(part);
            }
        }
        return token("ADDR", value.toString(), UnaryOperator.identity());
    }

    private static String trimSpaces(String value) {
        return value == null ? null : value.replaceAll("^ +| +$", "");
    }

    private static String token(String prefix, String value, UnaryOperator<String> normalize) {
        if (value == null || value.isBlank()) {
            return null;
        }
        if (value.matches("^" + prefix + "_SHA256_[A-Fa-f0-9]{64}$")) {
            return value.toUpperCase(Locale.ROOT);
        }

        String normalized = normalize.apply(value);
        if (normalized.isEmpty()) {
            return null;
        }
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(normalized.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexadecimal = new StringBuilder(64);
            for (byte item : hash) {
                hexadecimal.append(String.format("%02X", item));
            }
            return prefix + "_SHA256_" + hexadecimal;
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
