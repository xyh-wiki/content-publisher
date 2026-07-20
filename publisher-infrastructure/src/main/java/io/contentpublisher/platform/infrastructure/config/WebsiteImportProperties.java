package io.contentpublisher.platform.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("publisher.website")
public record WebsiteImportProperties(Duration timeout, int maxResponseBytes, int maxTextCharacters,
                                      int minTextCharacters) {
    public WebsiteImportProperties {
        timeout = timeout == null ? Duration.ofSeconds(20) : timeout;
        if (maxResponseBytes < 64_000 || maxResponseBytes > 5_000_000) maxResponseBytes = 2_000_000;
        if (maxTextCharacters < 10_000 || maxTextCharacters > 500_000) maxTextCharacters = 100_000;
        if (minTextCharacters < 1 || minTextCharacters > 1_000) minTextCharacters = 20;
    }
}
