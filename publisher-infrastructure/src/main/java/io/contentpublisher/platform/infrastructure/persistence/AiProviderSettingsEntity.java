package io.contentpublisher.platform.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.math.BigDecimal;

@Entity
@Table(name = "ai_provider_settings")
class AiProviderSettingsEntity {
    @Id @Column(name = "tenant_id", length = 100) String tenantId;
    @Column(name = "base_url", nullable = false, length = 2048) String baseUrl;
    @Column(name = "encrypted_api_key", columnDefinition = "text") String encryptedApiKey;
    @Column(name = "api_key_fingerprint", length = 64) String apiKeyFingerprint;
    @Column(nullable = false, length = 200) String model;
    @Column(name = "timeout_seconds", nullable = false) int timeoutSeconds;
    @Column(nullable = false, precision = 4, scale = 3) BigDecimal temperature;
    @Column(nullable = false) boolean enabled;
    @Column(name = "settings_version", nullable = false) int settingsVersion;
    @Column(name = "created_by", nullable = false, length = 200) String createdBy;
    @Column(name = "updated_by", nullable = false, length = 200) String updatedBy;
    @Column(name = "created_at", nullable = false) Instant createdAt;
    @Column(name = "updated_at", nullable = false) Instant updatedAt;

    protected AiProviderSettingsEntity() {}
}
