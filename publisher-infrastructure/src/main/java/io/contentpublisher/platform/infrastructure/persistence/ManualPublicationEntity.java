package io.contentpublisher.platform.infrastructure.persistence;

import io.contentpublisher.platform.domain.ChannelType;
import io.contentpublisher.platform.domain.ContentFormat;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "manual_publications")
class ManualPublicationEntity {
    @Id UUID id;
    @Column(name = "tenant_id", nullable = false, length = 100) String tenantId;
    @Column(name = "article_id", nullable = false) UUID articleId;
    @Enumerated(EnumType.STRING) @Column(name = "channel_type", nullable = false, length = 40) ChannelType channelType;
    @Enumerated(EnumType.STRING) @Column(name = "content_format", nullable = false, length = 30) ContentFormat contentFormat;
    @Column(name = "adapted_title", nullable = false, length = 500) String adaptedTitle;
    @Column(name = "adapted_content", nullable = false, columnDefinition = "text") String adaptedContent;
    @Column(name = "external_url", nullable = false, length = 2048) String externalUrl;
    @Column(name = "published_by", nullable = false, length = 200) String publishedBy;
    @Column(name = "published_at", nullable = false) Instant publishedAt;
    @Column(name = "deleted_at") Instant deletedAt;
    @Column(name = "deleted_by", length = 200) String deletedBy;

    protected ManualPublicationEntity() {
    }
}
