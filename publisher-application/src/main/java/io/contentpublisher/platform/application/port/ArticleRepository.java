package io.contentpublisher.platform.application.port;

import io.contentpublisher.platform.domain.Article;
import io.contentpublisher.platform.domain.ArticleVersion;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ArticleRepository {
    Article save(Article article);
    Article saveWithVersion(Article article, ArticleVersion version);
    Optional<Article> findArticleById(String tenantId, UUID id);
    Optional<Article> findByGenerationJobId(String tenantId, UUID generationJobId);
    List<Article> findRecentArticles(String tenantId, int limit);
    List<Article> findRecentByProjectId(String tenantId, UUID projectId, int limit);
    List<ArticleVersion> findVersions(String tenantId, UUID articleId);
}
