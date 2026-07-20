package io.contentpublisher.platform.web.dto;

import io.contentpublisher.platform.domain.Article;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ArticleResponse(UUID id, UUID projectId, String title, String summary, String markdown,
                              List<String> keywords, String language, String sourceRevision, String status,
                              int currentVersion,
                              Instant createdAt, Instant updatedAt) {
    public static ArticleResponse from(Article article) {
        return new ArticleResponse(article.id(), article.projectId(), article.title(), article.summary(), article.markdown(),
                article.keywords(), article.language(), article.sourceRevision(), article.status().name(),
                article.currentVersion(),
                article.createdAt(), article.updatedAt());
    }
}
