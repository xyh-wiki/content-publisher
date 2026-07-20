package io.contentpublisher.platform.web.dto;

import io.contentpublisher.platform.domain.Article;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ArticleResponse(UUID id, UUID projectId, String sourceType, String sourceUrl, String sourceTitle,
                              String sourceDescription, String targetAudience, String articleType,
                              String knowledgeLevel, String title, String summary, String markdown,
                              List<String> tags, List<String> keywords, String language, String sourceRevision, String status,
                              int currentVersion,
                              Instant createdAt, Instant updatedAt) {
    public static ArticleResponse from(Article article) {
        return new ArticleResponse(article.id(), article.projectId(), article.sourceType().name(),
                article.origin().sourceUrl(), article.origin().title(),
                article.origin().description(), article.origin().audience(), article.origin().articleType(),
                article.origin().knowledgeLevel(), article.title(), article.summary(), article.markdown(),
                article.tags(), article.keywords(), article.language(), article.sourceRevision(), article.status().name(),
                article.currentVersion(),
                article.createdAt(), article.updatedAt());
    }
}
