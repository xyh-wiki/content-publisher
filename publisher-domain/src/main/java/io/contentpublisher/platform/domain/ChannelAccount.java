package io.contentpublisher.platform.domain;

import java.time.Instant;
import java.util.UUID;

public record ChannelAccount(
        UUID id,
        String tenantId,
        ChannelType type,
        String displayName,
        String baseUrl,
        String encryptedCredentials,
        String idempotencyKey,
        String requestHash,
        String credentialFingerprint,
        int version,
        ChannelAccountStatus status,
        String createdBy,
        String updatedBy,
        Instant createdAt,
        Instant updatedAt) {
    public ChannelAccount {
        if (version < 1) throw new IllegalArgumentException("渠道账号版本号必须大于零");
    }
}
