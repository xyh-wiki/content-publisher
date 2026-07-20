package io.contentpublisher.platform.domain;

import java.time.Instant;
import java.util.UUID;

public record Publication(
        UUID id,
        String tenantId,
        UUID articleId,
        UUID channelAccountId,
        UUID publicationJobId,
        ChannelType channelType,
        String canonicalUrl,
        PublicationStatus status,
        String externalId,
        String externalUrl,
        String errorCode,
        String errorMessage,
        Instant publishedAt,
        Instant createdAt,
        Instant updatedAt) {
}
