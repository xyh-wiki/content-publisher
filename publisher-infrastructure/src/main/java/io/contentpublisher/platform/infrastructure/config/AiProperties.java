package io.contentpublisher.platform.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Duration;

@ConfigurationProperties("publisher.ai")
public record AiProperties(
        boolean enabled,
        URI baseUrl,
        String apiKey,
        String model,
        Duration timeout,
        double temperature) {

    public AiProperties {
        baseUrl = baseUrl == null ? URI.create("http://127.0.0.1:11434/v1") : baseUrl;
        model = model == null || model.isBlank() ? "qwen3:14b" : model;
        timeout = timeout == null ? Duration.ofSeconds(90) : timeout;
        if (temperature < 0 || temperature > 1) {
            throw new IllegalArgumentException("publisher.ai.temperature 必须在 0 到 1 之间");
        }
    }
}
