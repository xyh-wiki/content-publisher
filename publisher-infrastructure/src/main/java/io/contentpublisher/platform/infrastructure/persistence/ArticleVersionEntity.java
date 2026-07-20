package io.contentpublisher.platform.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "article_versions")
class ArticleVersionEntity {
    @EmbeddedId ArticleVersionKey id;
    @Column(name = "tenant_id", nullable = false, length = 100) String tenantId;
    @Column(nullable = false, length = 500) String title;
    @Column(nullable = false, length = 2000) String summary;
    @Column(nullable = false, columnDefinition = "text") String markdown;
    @Column(name = "tags_json", nullable = false, columnDefinition = "text") String tagsJson;
    @Column(name = "keywords_json", nullable = false, columnDefinition = "text") String keywordsJson;
    @Column(name = "created_by", nullable = false, length = 200) String createdBy;
    @Column(name = "created_at", nullable = false) Instant createdAt;

    protected ArticleVersionEntity() {}
}
