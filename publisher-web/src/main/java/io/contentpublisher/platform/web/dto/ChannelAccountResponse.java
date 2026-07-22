package io.contentpublisher.platform.web.dto;

import io.contentpublisher.platform.domain.ChannelAccount;

import java.time.Instant;
import java.util.UUID;

public record ChannelAccountResponse(UUID id, String type, String displayName, String baseUrl, int version, String status,
                                     String verificationStatus, String verificationMessage, Instant lastVerifiedAt,
                                     Instant createdAt, Instant updatedAt) {
    public static ChannelAccountResponse from(ChannelAccount account) {
        return new ChannelAccountResponse(account.id(), account.type().name(), account.displayName(), account.baseUrl(),
                account.version(), account.status().name(), account.verificationStatus() == null ? null
                : account.verificationStatus().name(), account.verificationMessage(), account.lastVerifiedAt(),
                account.createdAt(), account.updatedAt());
    }
}
