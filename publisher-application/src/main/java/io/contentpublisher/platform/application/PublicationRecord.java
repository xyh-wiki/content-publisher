package io.contentpublisher.platform.application;

import io.contentpublisher.platform.domain.ChannelType;
import io.contentpublisher.platform.domain.ContentFormat;
import io.contentpublisher.platform.domain.PublicationStatus;

import java.time.Instant;
import java.util.UUID;

public record PublicationRecord(
        UUID id,
        UUID articleId,
        ChannelType channelType,
        PublicationMethod method,
        PublicationStatus status,
        UUID channelAccountId,
        String channelAccountName,
        ContentFormat contentFormat,
        String externalUrl,
        String errorCode,
        String errorMessage,
        String publishedBy,
        Instant createdAt,
        Instant updatedAt) {
}
