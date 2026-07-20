package io.contentpublisher.platform.application.port;

import io.contentpublisher.platform.domain.Article;

import java.util.Optional;
import java.util.UUID;

public interface ArticleRepository {
    Article save(Article article);
    Optional<Article> findArticleById(String tenantId, UUID id);
    Optional<Article> findByGenerationJobId(String tenantId, UUID generationJobId);
}
