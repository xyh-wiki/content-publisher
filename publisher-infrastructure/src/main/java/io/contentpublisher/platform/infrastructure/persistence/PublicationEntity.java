package io.contentpublisher.platform.infrastructure.persistence;

import io.contentpublisher.platform.domain.ChannelType;
import io.contentpublisher.platform.domain.PublicationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "publications")
class PublicationEntity {
    @Id UUID id;
    @Column(name = "tenant_id", nullable = false, length = 100) String tenantId;
    @Column(name = "article_id", nullable = false) UUID articleId;
    @Column(name = "channel_account_id", nullable = false) UUID channelAccountId;
    @Column(name = "publication_job_id", nullable = false, unique = true) UUID publicationJobId;
    @Enumerated(EnumType.STRING) @Column(name = "channel_type", nullable = false, length = 40) ChannelType channelType;
    @Column(name = "canonical_url", length = 2048) String canonicalUrl;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) PublicationStatus status;
    @Column(name = "external_id", length = 500) String externalId;
    @Column(name = "external_url", length = 2048) String externalUrl;
    @Column(name = "error_code", length = 100) String errorCode;
    @Column(name = "error_message", length = 2000) String errorMessage;
    @Column(name = "published_at") Instant publishedAt;
    @Column(name = "created_at", nullable = false) Instant createdAt;
    @Column(name = "updated_at", nullable = false) Instant updatedAt;

    protected PublicationEntity() {}
}
