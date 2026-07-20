package io.contentpublisher.platform.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("publisher.secrets")
public record SecretProperties(String encryptionKey) {
}
