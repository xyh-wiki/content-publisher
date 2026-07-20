package io.contentpublisher.platform.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record Article(
        UUID id,
        String tenantId,
        UUID projectId,
        UUID generationJobId,
        String title,
        String summary,
        String markdown,
        List<String> keywords,
        String language,
        String sourceRevision,
        int currentVersion,
        ArticleStatus status,
        String createdBy,
        String updatedBy,
        Instant createdAt,
        Instant updatedAt) {
    public Article {
        keywords = keywords == null ? List.of() : List.copyOf(keywords);
        if (currentVersion < 1) throw new IllegalArgumentException("文章版本号必须大于零");
    }
}
