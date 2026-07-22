package io.contentpublisher.platform.application;

import java.util.UUID;

public record PublicationPreflightResult(UUID channelAccountId, boolean ready, String message) {
    public static PublicationPreflightResult ready(UUID accountId, String message) {
        return new PublicationPreflightResult(accountId, true, message);
    }

    public static PublicationPreflightResult blocked(UUID accountId, String message) {
        return new PublicationPreflightResult(accountId, false, message);
    }
}
