package io.contentpublisher.platform.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "articles")
class ArticleEntity {
    @Id UUID id;
    @Column(name = "tenant_id", nullable = false, length = 100) String tenantId;
    @Column(name = "project_id") UUID projectId;
    @Column(name = "source_type", nullable = false, length = 20) String sourceType;
    @Column(name = "source_title", length = 300) String sourceTitle;
    @Column(name = "source_url", length = 2048) String sourceUrl;
    @Column(name = "source_description", length = 4000) String sourceDescription;
    @Column(name = "target_audience", length = 500) String targetAudience;
    @Column(name = "article_type", length = 50) String articleType;
    @Column(name = "knowledge_level", length = 30) String knowledgeLevel;
    @Column(name = "source_keywords_json", nullable = false, columnDefinition = "text") String sourceKeywordsJson;
    @Column(name = "generation_job_id", unique = true) UUID generationJobId;
    @Column(nullable = false, length = 500) String title;
    @Column(nullable = false, length = 2000) String summary;
    @Column(nullable = false, columnDefinition = "text") String markdown;
    @Column(name = "tags_json", nullable = false, columnDefinition = "text") String tagsJson;
    @Column(name = "keywords_json", nullable = false, columnDefinition = "text") String keywordsJson;
    @Column(name = "title_en", nullable = false, length = 500) String titleEn;
    @Column(name = "summary_en", nullable = false, length = 2000) String summaryEn;
    @Column(name = "markdown_en", nullable = false, columnDefinition = "text") String markdownEn;
    @Column(name = "tags_en_json", nullable = false, columnDefinition = "text") String tagsEnJson;
    @Column(name = "keywords_en_json", nullable = false, columnDefinition = "text") String keywordsEnJson;
    @Column(nullable = false, length = 20) String language;
    @Column(name = "source_revision", nullable = false, length = 64) String sourceRevision;
    @Column(name = "current_version", nullable = false) int currentVersion;
    @Column(nullable = false, length = 30) String status;
    @Column(name = "created_by", nullable = false, length = 200) String createdBy;
    @Column(name = "updated_by", nullable = false, length = 200) String updatedBy;
    @Column(name = "created_at", nullable = false) Instant createdAt;
    @Column(name = "updated_at", nullable = false) Instant updatedAt;
    @Column(name = "deleted_at") Instant deletedAt;
    @Column(name = "deleted_by", length = 200) String deletedBy;

    protected ArticleEntity() {}
}
