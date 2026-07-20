package io.contentpublisher.platform.web.dto;

import io.contentpublisher.platform.domain.ArticleVersion;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ArticleVersionResponse(UUID articleId, int versionNumber, String title, String summary, String markdown,
                                     List<String> tags, List<String> keywords, String createdBy, Instant createdAt) {
    public static ArticleVersionResponse from(ArticleVersion version) {
        return new ArticleVersionResponse(version.articleId(), version.versionNumber(), version.title(),
                version.summary(), version.markdown(), version.tags(), version.keywords(), version.createdBy(),
                version.createdAt());
    }
}
