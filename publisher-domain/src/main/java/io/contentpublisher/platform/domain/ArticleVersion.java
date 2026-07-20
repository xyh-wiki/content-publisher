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
        String titleEn,
        String summaryEn,
        String markdownEn,
        List<String> tagsEn,
        List<String> keywordsEn,
        String createdBy,
        Instant createdAt) {
    public ArticleVersion {
        tags = tags == null ? List.of() : List.copyOf(tags);
        keywords = keywords == null ? List.of() : List.copyOf(keywords);
        titleEn = titleEn == null ? "" : titleEn;
        summaryEn = summaryEn == null ? "" : summaryEn;
        markdownEn = markdownEn == null ? "" : markdownEn;
        tagsEn = tagsEn == null ? List.of() : List.copyOf(tagsEn);
        keywordsEn = keywordsEn == null ? List.of() : List.copyOf(keywordsEn);
        if (versionNumber < 1) throw new IllegalArgumentException("文章版本号必须大于零");
    }

    public ArticleVersion(String tenantId, UUID articleId, int versionNumber, String title, String summary,
                          String markdown, List<String> keywords, String createdBy, Instant createdAt) {
        this(tenantId, articleId, versionNumber, title, summary, markdown, keywords, keywords,
                "", "", "", List.of(), List.of(), createdBy, createdAt);
    }

    public ArticleVersion(String tenantId, UUID articleId, int versionNumber, String title, String summary,
                          String markdown, List<String> tags, List<String> keywords, String createdBy,
                          Instant createdAt) {
        this(tenantId, articleId, versionNumber, title, summary, markdown, tags, keywords,
                "", "", "", List.of(), List.of(), createdBy, createdAt);
    }
}
