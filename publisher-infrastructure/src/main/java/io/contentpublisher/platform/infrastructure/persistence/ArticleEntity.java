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
    @Column(name = "project_id", nullable = false) UUID projectId;
    @Column(name = "generation_job_id", unique = true) UUID generationJobId;
    @Column(nullable = false, length = 500) String title;
    @Column(nullable = false, length = 2000) String summary;
    @Column(nullable = false, columnDefinition = "text") String markdown;
    @Column(name = "keywords_json", nullable = false, columnDefinition = "text") String keywordsJson;
    @Column(nullable = false, length = 20) String language;
    @Column(name = "source_revision", nullable = false, length = 64) String sourceRevision;
    @Column(nullable = false, length = 30) String status;
    @Column(name = "created_by", nullable = false, length = 200) String createdBy;
    @Column(name = "updated_by", nullable = false, length = 200) String updatedBy;
    @Column(name = "created_at", nullable = false) Instant createdAt;
    @Column(name = "updated_at", nullable = false) Instant updatedAt;

    protected ArticleEntity() {}
}
