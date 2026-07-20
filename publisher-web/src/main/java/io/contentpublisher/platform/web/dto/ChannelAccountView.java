package io.contentpublisher.platform.web.dto;

import io.contentpublisher.platform.domain.ChannelAccount;
import io.contentpublisher.platform.domain.ChannelAccountStatus;
import io.contentpublisher.platform.domain.ChannelType;

import java.time.Instant;
import java.util.UUID;

public record ChannelAccountView(
        UUID id,
        ChannelType type,
        String displayName,
        String baseUrl,
        int version,
        ChannelAccountStatus status,
        Instant updatedAt) {
    public static ChannelAccountView from(ChannelAccount account) {
        return new ChannelAccountView(account.id(), account.type(), account.displayName(), account.baseUrl(),
                account.version(), account.status(), account.updatedAt());
    }
}
