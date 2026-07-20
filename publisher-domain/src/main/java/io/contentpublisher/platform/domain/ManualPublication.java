package io.contentpublisher.platform.domain;

import java.time.Instant;
import java.util.UUID;

public record ManualPublication(
        UUID id,
        String tenantId,
        UUID articleId,
        ChannelType channelType,
        ContentFormat contentFormat,
        String adaptedTitle,
        String adaptedContent,
        String externalUrl,
        String publishedBy,
        Instant publishedAt) {
}
