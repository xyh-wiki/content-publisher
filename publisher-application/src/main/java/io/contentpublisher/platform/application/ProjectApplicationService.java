package io.contentpublisher.platform.application;

import io.contentpublisher.platform.application.port.ArticleRepository;
import io.contentpublisher.platform.application.port.AuditRecorder;
import io.contentpublisher.platform.application.port.ContentGenerator;
import io.contentpublisher.platform.application.port.ProjectRepository;
import io.contentpublisher.platform.application.port.RepositoryInspector;
import io.contentpublisher.platform.application.port.RepositorySnapshotStore;
import io.contentpublisher.platform.application.port.WebsiteInspector;
import io.contentpublisher.platform.domain.ActorContext;
import io.contentpublisher.platform.domain.Article;
import io.contentpublisher.platform.domain.GenerationPolicy;
import io.contentpublisher.platform.domain.Project;
import io.contentpublisher.platform.domain.TopicBrief;
import io.contentpublisher.platform.domain.WebsiteBrief;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

/**
 * Stable facade for project queries, repository imports and content generation.
 * Write-side responsibilities live in the dedicated services and can evolve independently.
 */
public final class ProjectApplicationService {
    private final ProjectRepository projects;
    private final ArticleRepository articles;
    private final ProjectImportApplicationService imports;
    private final ContentGenerationApplicationService generation;

    public ProjectApplicationService(ProjectRepository projects, ArticleRepository articles,
                                     ProjectImportApplicationService imports,
                                     ContentGenerationApplicationService generation) {
        this.projects = projects;
        this.articles = articles;
        this.imports = imports;
        this.generation = generation;
    }

    public ProjectApplicationService(ProjectRepository projects, ArticleRepository articles,
                                     RepositorySnapshotStore snapshots, RepositoryInspector repositoryInspector,
                                     WebsiteInspector websiteInspector, ContentGenerator contentGenerator,
                                     AuditRecorder auditRecorder, Clock clock) {
        this(projects, articles,
                new ProjectImportApplicationService(projects, snapshots, repositoryInspector, auditRecorder, clock),
                new ContentGenerationApplicationService(projects, articles, snapshots, websiteInspector,
                        contentGenerator, auditRecorder, clock));
    }

    public Project importProject(ActorContext actor, String gitUrl, String branch) {
        return imports.importProject(actor, gitUrl, branch);
    }

    public Project importProject(ActorContext actor, String gitUrl, String branch, JobProgressReporter progress) {
        return imports.importProject(actor, gitUrl, branch, progress);
    }

    public Project getProject(ActorContext actor, UUID id) {
        return projects.findProjectById(actor.tenantId(), id)
                .orElseThrow(() -> new ApplicationException("PROJECT_NOT_FOUND", "项目不存在"));
    }

    public List<Project> listProjects(ActorContext actor, int limit) {
        return projects.findRecentProjects(actor.tenantId(), requireListLimit(limit));
    }

    public List<Article> listArticles(ActorContext actor, int limit) {
        return articles.findRecentArticles(actor.tenantId(), requireListLimit(limit));
    }

    public List<Article> listProjectArticles(ActorContext actor, UUID projectId, int limit) {
        getProject(actor, projectId);
        return articles.findRecentByProjectId(actor.tenantId(), projectId, requireListLimit(limit));
    }

    public Article generateArticle(ActorContext actor, UUID projectId, GenerationPolicy policy) {
        return generation.generateArticle(actor, projectId, policy);
    }

    public Article generateArticle(ActorContext actor, UUID projectId, GenerationPolicy policy, UUID generationJobId) {
        return generation.generateArticle(actor, projectId, policy, generationJobId);
    }

    public Article generateArticle(ActorContext actor, UUID projectId, GenerationPolicy policy, UUID generationJobId,
                                   JobProgressReporter progress) {
        return generation.generateArticle(actor, projectId, policy, generationJobId, progress);
    }

    public Article generateTopicArticle(ActorContext actor, TopicBrief brief, GenerationPolicy policy,
                                        UUID generationJobId) {
        return generation.generateTopicArticle(actor, brief, policy, generationJobId);
    }

    public Article generateTopicArticle(ActorContext actor, TopicBrief brief, GenerationPolicy policy,
                                        UUID generationJobId, JobProgressReporter progress) {
        return generation.generateTopicArticle(actor, brief, policy, generationJobId, progress);
    }

    public Article generateWebsiteArticle(ActorContext actor, WebsiteBrief brief, GenerationPolicy policy,
                                          UUID generationJobId) {
        return generation.generateWebsiteArticle(actor, brief, policy, generationJobId);
    }

    public Article generateWebsiteArticle(ActorContext actor, WebsiteBrief brief, GenerationPolicy policy,
                                          UUID generationJobId, JobProgressReporter progress) {
        return generation.generateWebsiteArticle(actor, brief, policy, generationJobId, progress);
    }

    public Article getArticle(ActorContext actor, UUID articleId) {
        return articles.findArticleById(actor.tenantId(), articleId)
                .orElseThrow(() -> new ApplicationException("ARTICLE_NOT_FOUND", "文章不存在"));
    }

    private int requireListLimit(int limit) {
        if (limit < 1 || limit > 100) throw new IllegalArgumentException("列表查询数量必须在 1 到 100 之间");
        return limit;
    }
}
