package io.contentpublisher.platform.web.dto;

import io.contentpublisher.platform.domain.ArticleVersion;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ArticleVersionResponse(UUID articleId, int versionNumber, String title, String summary, String markdown,
                                     List<String> tags, List<String> keywords, String titleEn, String summaryEn,
                                     String markdownEn, List<String> tagsEn, List<String> keywordsEn,
                                     String createdBy, Instant createdAt) {
    public static ArticleVersionResponse from(ArticleVersion version) {
        return new ArticleVersionResponse(version.articleId(), version.versionNumber(), version.title(),
                version.summary(), version.markdown(), version.tags(), version.keywords(), version.titleEn(),
                version.summaryEn(), version.markdownEn(), version.tagsEn(), version.keywordsEn(),
                version.createdBy(), version.createdAt());
    }
}
