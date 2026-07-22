package io.contentpublisher.platform.web.dto;

import io.contentpublisher.platform.domain.ChannelAccount;
import io.contentpublisher.platform.domain.ChannelAccountStatus;
import io.contentpublisher.platform.domain.ChannelType;
import io.contentpublisher.platform.domain.ChannelVerificationStatus;

import java.time.Instant;
import java.util.UUID;

public record ChannelAccountView(
        UUID id,
        ChannelType type,
        String displayName,
        String baseUrl,
        int version,
        ChannelAccountStatus status,
        ChannelVerificationStatus verificationStatus,
        String verificationMessage,
        Instant lastVerifiedAt,
        Instant updatedAt) {
    public static ChannelAccountView from(ChannelAccount account) {
        return new ChannelAccountView(account.id(), account.type(), account.displayName(), account.baseUrl(),
                account.version(), account.status(), account.verificationStatus(), account.verificationMessage(),
                account.lastVerifiedAt(), account.updatedAt());
    }
}
