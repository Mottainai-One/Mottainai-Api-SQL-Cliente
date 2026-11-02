package com.institutojf.mottainai.client.dto.response;

/** Credencial curta para a caixa Redis; nunca contém a chave usada pelo servidor para assiná-la. */
public record NotificationAccessTokenResponse(String accessToken, long expiresIn) {
    /** Impede a exposição acidental da credencial em logs de respostas. */
    @Override public String toString() {
        return "NotificationAccessTokenResponse[accessToken=[REDACTED], expiresIn=" + expiresIn + "]";
    }
}
