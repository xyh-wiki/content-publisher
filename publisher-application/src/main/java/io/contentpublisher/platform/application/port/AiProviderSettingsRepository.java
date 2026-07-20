package io.contentpublisher.platform.application.port;

import io.contentpublisher.platform.domain.AiProviderSettings;

import java.util.Optional;

public interface AiProviderSettingsRepository {
    Optional<AiProviderSettings> findByTenantId(String tenantId);
    AiProviderSettings save(AiProviderSettings settings);
    Optional<AiProviderSettings> updateIfVersionMatches(AiProviderSettings settings, int expectedVersion);
}
