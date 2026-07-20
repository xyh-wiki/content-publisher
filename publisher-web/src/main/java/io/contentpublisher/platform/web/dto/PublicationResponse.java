package io.contentpublisher.platform.web.dto;

import io.contentpublisher.platform.domain.Publication;

import java.time.Instant;
import java.util.UUID;

public record PublicationResponse(UUID id, UUID articleId, UUID channelAccountId, String channelType, String status,
                                  String canonicalUrl, String externalId, String externalUrl, String errorCode, String errorMessage,
                                  Instant publishedAt, Instant createdAt, Instant updatedAt) {
    public static PublicationResponse from(Publication publication) {
        return new PublicationResponse(publication.id(), publication.articleId(), publication.channelAccountId(),
                publication.channelType().name(), publication.status().name(), publication.canonicalUrl(), publication.externalId(),
                publication.externalUrl(), publication.errorCode(), publication.errorMessage(),
                publication.publishedAt(), publication.createdAt(), publication.updatedAt());
    }
}
