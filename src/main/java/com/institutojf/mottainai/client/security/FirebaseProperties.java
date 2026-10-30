package com.institutojf.mottainai.client.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.firebase")
public record FirebaseProperties(String projectId) {
}
