package io.contentpublisher.platform.domain;

import java.time.Instant;

public record AiProviderSettings(
        String tenantId,
        String baseUrl,
        String encryptedApiKey,
        String apiKeyFingerprint,
        String model,
        int timeoutSeconds,
        double temperature,
        boolean enabled,
        int version,
        String createdBy,
        String updatedBy,
        Instant createdAt,
        Instant updatedAt) {

    public AiProviderSettings {
        if (version < 1) throw new IllegalArgumentException("AI 配置版本必须大于零");
        if (timeoutSeconds < 5 || timeoutSeconds > 300) {
            throw new IllegalArgumentException("AI 请求超时必须在 5 到 300 秒之间");
        }
        if (temperature < 0 || temperature > 1) {
            throw new IllegalArgumentException("AI 温度必须在 0 到 1 之间");
        }
    }

    public boolean apiKeyConfigured() {
        return encryptedApiKey != null && !encryptedApiKey.isBlank();
    }
}
