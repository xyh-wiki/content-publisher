package io.contentpublisher.platform.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ArticleVersion(
        String tenantId,
        UUID articleId,
        int versionNumber,
        String title,
        String summary,
        String markdown,
        List<String> tags,
        List<String> keywords,
        String createdBy,
        Instant createdAt) {
    public ArticleVersion {
        tags = tags == null ? List.of() : List.copyOf(tags);
        keywords = keywords == null ? List.of() : List.copyOf(keywords);
        if (versionNumber < 1) throw new IllegalArgumentException("文章版本号必须大于零");
    }

    public ArticleVersion(String tenantId, UUID articleId, int versionNumber, String title, String summary,
                          String markdown, List<String> keywords, String createdBy, Instant createdAt) {
        this(tenantId, articleId, versionNumber, title, summary, markdown, keywords, keywords, createdBy, createdAt);
    }
}
