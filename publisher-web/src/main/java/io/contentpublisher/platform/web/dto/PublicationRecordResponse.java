package io.contentpublisher.platform.web.dto;

import io.contentpublisher.platform.application.PublicationRecord;

import java.time.Instant;
import java.util.UUID;

public record PublicationRecordResponse(
        UUID id,
        UUID articleId,
        String channelType,
        String method,
        String status,
        UUID channelAccountId,
        String channelAccountName,
        String contentFormat,
        String externalUrl,
        String errorCode,
        String errorMessage,
        String publishedBy,
        Instant createdAt,
        Instant updatedAt) {
    public static PublicationRecordResponse from(PublicationRecord record) {
        return new PublicationRecordResponse(record.id(), record.articleId(), record.channelType().name(),
                record.method().name(), record.status().name(), record.channelAccountId(),
                record.channelAccountName(), record.contentFormat().name(), record.externalUrl(), record.errorCode(),
                record.errorMessage(), record.publishedBy(), record.createdAt(), record.updatedAt());
    }
}
