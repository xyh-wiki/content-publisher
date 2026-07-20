package io.contentpublisher.platform.application;

import io.contentpublisher.platform.application.port.AiEndpointPolicy;
import io.contentpublisher.platform.application.port.AiProviderSettingsRepository;
import io.contentpublisher.platform.application.port.AuditRecorder;
import io.contentpublisher.platform.application.port.SecretCipher;
import io.contentpublisher.platform.domain.ActorContext;
import io.contentpublisher.platform.domain.AiProviderSettings;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class AiSettingsApplicationService {
    private final AiProviderSettingsRepository settingsRepository;
    private final AiEndpointPolicy endpointPolicy;
    private final SecretCipher secretCipher;
    private final AuditRecorder auditRecorder;
    private final Clock clock;

    public AiSettingsApplicationService(AiProviderSettingsRepository settingsRepository,
                                        AiEndpointPolicy endpointPolicy,
                                        SecretCipher secretCipher,
                                        AuditRecorder auditRecorder,
                                        Clock clock) {
        this.settingsRepository = settingsRepository;
        this.endpointPolicy = endpointPolicy;
        this.secretCipher = secretCipher;
        this.auditRecorder = auditRecorder;
        this.clock = clock;
    }

    public Optional<AiProviderSettings> getSettings(ActorContext actor) {
        return settingsRepository.findByTenantId(actor.tenantId());
    }

    public AiProviderSettings save(ActorContext actor, int expectedVersion, boolean enabled, String baseUrl,
                                   String model, int timeoutSeconds, double temperature,
                                   String apiKey, boolean clearApiKey) {
        String normalizedBaseUrl = endpointPolicy.validateAndNormalize(baseUrl);
        String normalizedModel = requireText(model, "AI 模型不能为空", 200);
        if (timeoutSeconds < 5 || timeoutSeconds > 300) {
            throw new ApplicationException("AI_SETTINGS_INVALID", "AI 请求超时必须在 5 到 300 秒之间");
        }
        if (temperature < 0 || temperature > 1) {
            throw new ApplicationException("AI_SETTINGS_INVALID", "AI 温度必须在 0 到 1 之间");
        }
        if (apiKey != null && apiKey.length() > 8000) {
            throw new ApplicationException("AI_SETTINGS_INVALID", "API Key 长度不能超过 8000 个字符");
        }

        AiProviderSettings existing = settingsRepository.findByTenantId(actor.tenantId()).orElse(null);
        Instant now = clock.instant();
        String encryptedApiKey;
        String fingerprint;
        String submittedApiKey = apiKey == null ? "" : apiKey.trim();
        if (clearApiKey) {
            encryptedApiKey = null;
            fingerprint = null;
        } else if (!submittedApiKey.isEmpty()) {
            encryptedApiKey = secretCipher.encrypt(secretContext(actor.tenantId()), submittedApiKey);
            fingerprint = secretCipher.fingerprint(submittedApiKey);
        } else {
            encryptedApiKey = existing == null ? null : existing.encryptedApiKey();
            fingerprint = existing == null ? null : existing.apiKeyFingerprint();
        }

        AiProviderSettings saved;
        if (existing == null) {
            if (expectedVersion != 0) throw versionConflict();
            saved = settingsRepository.save(new AiProviderSettings(actor.tenantId(), normalizedBaseUrl,
                    encryptedApiKey, fingerprint, normalizedModel, timeoutSeconds, temperature, enabled, 1,
                    actor.subject(), actor.subject(), now, now));
        } else {
            if (existing.version() != expectedVersion) throw versionConflict();
            AiProviderSettings candidate = new AiProviderSettings(existing.tenantId(), normalizedBaseUrl,
                    encryptedApiKey, fingerprint, normalizedModel, timeoutSeconds, temperature, enabled,
                    existing.version() + 1, existing.createdBy(), actor.subject(), existing.createdAt(), now);
            saved = settingsRepository.updateIfVersionMatches(candidate, expectedVersion)
                    .orElseThrow(this::versionConflict);
        }

        auditRecorder.record(actor, "AI_SETTINGS_UPDATED", "AI_PROVIDER_SETTINGS", settingsId(actor.tenantId()),
                Map.of("host", URI.create(saved.baseUrl()).getHost(), "model", saved.model(),
                        "enabled", Boolean.toString(saved.enabled()), "version", Integer.toString(saved.version()),
                        "apiKeyConfigured", Boolean.toString(saved.apiKeyConfigured())));
        return saved;
    }

    private String requireText(String value, String message, int maxLength) {
        if (value == null || value.isBlank()) throw new ApplicationException("AI_SETTINGS_INVALID", message);
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new ApplicationException("AI_SETTINGS_INVALID", "字段长度超过限制");
        }
        return normalized;
    }

    private ApplicationException versionConflict() {
        return new ApplicationException("AI_SETTINGS_VERSION_CONFLICT", "AI 配置已被其他请求修改，请刷新后重试");
    }

    private String secretContext(String tenantId) {
        return "ai-api-key:" + tenantId;
    }

    private UUID settingsId(String tenantId) {
        return UUID.nameUUIDFromBytes(("ai-settings:" + tenantId).getBytes(StandardCharsets.UTF_8));
    }
}
