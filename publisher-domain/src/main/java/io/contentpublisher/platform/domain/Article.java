package io.contentpublisher.platform.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record Article(
        UUID id,
        String tenantId,
        ContentOrigin origin,
        UUID generationJobId,
        String title,
        String summary,
        String markdown,
        List<String> tags,
        List<String> keywords,
        String titleEn,
        String summaryEn,
        String markdownEn,
        List<String> tagsEn,
        List<String> keywordsEn,
        String language,
        String sourceRevision,
        int currentVersion,
        ArticleStatus status,
        String createdBy,
        String updatedBy,
        Instant createdAt,
        Instant updatedAt) {
    public Article {
        tags = tags == null ? List.of() : List.copyOf(tags);
        keywords = keywords == null ? List.of() : List.copyOf(keywords);
        titleEn = titleEn == null ? "" : titleEn;
        summaryEn = summaryEn == null ? "" : summaryEn;
        markdownEn = markdownEn == null ? "" : markdownEn;
        tagsEn = tagsEn == null ? List.of() : List.copyOf(tagsEn);
        keywordsEn = keywordsEn == null ? List.of() : List.copyOf(keywordsEn);
        if (origin == null) throw new IllegalArgumentException("文章来源不能为空");
        if (currentVersion < 1) throw new IllegalArgumentException("文章版本号必须大于零");
    }

    public Article(UUID id, String tenantId, UUID projectId, UUID generationJobId, String title, String summary,
                   String markdown, List<String> keywords, String language, String sourceRevision,
                   int currentVersion, ArticleStatus status, String createdBy, String updatedBy,
                   Instant createdAt, Instant updatedAt) {
        this(id, tenantId, ContentOrigin.git(projectId), generationJobId, title, summary, markdown, keywords, keywords,
                "", "", "", List.of(), List.of(), language, sourceRevision, currentVersion, status, createdBy,
                updatedBy, createdAt, updatedAt);
    }

    public Article(UUID id, String tenantId, ContentOrigin origin, UUID generationJobId, String title,
                   String summary, String markdown, List<String> keywords, String language, String sourceRevision,
                   int currentVersion, ArticleStatus status, String createdBy, String updatedBy,
                   Instant createdAt, Instant updatedAt) {
        this(id, tenantId, origin, generationJobId, title, summary, markdown, keywords, keywords,
                "", "", "", List.of(), List.of(), language, sourceRevision, currentVersion, status, createdBy,
                updatedBy, createdAt, updatedAt);
    }

    public UUID projectId() {
        return origin.projectId();
    }

    public ArticleSourceType sourceType() {
        return origin.type();
    }

    public boolean hasEnglishContent() {
        return !titleEn.isBlank() && !summaryEn.isBlank() && !markdownEn.isBlank();
    }
}
