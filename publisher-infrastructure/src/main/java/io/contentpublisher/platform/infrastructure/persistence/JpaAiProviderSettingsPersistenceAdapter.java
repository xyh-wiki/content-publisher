package io.contentpublisher.platform.infrastructure.persistence;

import io.contentpublisher.platform.application.port.AiProviderSettingsRepository;
import io.contentpublisher.platform.domain.AiProviderSettings;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
@Transactional
public class JpaAiProviderSettingsPersistenceAdapter implements AiProviderSettingsRepository {
    private final AiProviderSettingsJpaRepository settings;
    private final JpaDomainMapper mapper;

    public JpaAiProviderSettingsPersistenceAdapter(AiProviderSettingsJpaRepository settings, JpaDomainMapper mapper) {
        this.settings = settings;
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AiProviderSettings> findByTenantId(String tenantId) {
        return settings.findById(tenantId).map(mapper::aiSettings);
    }

    @Override
    public AiProviderSettings save(AiProviderSettings value) {
        AiProviderSettingsEntity entity = new AiProviderSettingsEntity();
        entity.tenantId = value.tenantId(); entity.baseUrl = value.baseUrl();
        entity.encryptedApiKey = value.encryptedApiKey(); entity.apiKeyFingerprint = value.apiKeyFingerprint();
        entity.model = value.model(); entity.timeoutSeconds = value.timeoutSeconds();
        entity.temperature = BigDecimal.valueOf(value.temperature()); entity.enabled = value.enabled();
        entity.settingsVersion = value.version(); entity.createdBy = value.createdBy();
        entity.updatedBy = value.updatedBy(); entity.createdAt = value.createdAt(); entity.updatedAt = value.updatedAt();
        return mapper.aiSettings(settings.save(entity));
    }

    @Override
    public Optional<AiProviderSettings> updateIfVersionMatches(AiProviderSettings value, int expectedVersion) {
        int updated = settings.updateIfVersionMatches(value.tenantId(), value.baseUrl(), value.encryptedApiKey(),
                value.apiKeyFingerprint(), value.model(), value.timeoutSeconds(),
                BigDecimal.valueOf(value.temperature()), value.enabled(), expectedVersion, value.version(),
                value.updatedBy(), value.updatedAt());
        if (updated == 0) return Optional.empty();
        return settings.findById(value.tenantId()).map(mapper::aiSettings);
    }
}
