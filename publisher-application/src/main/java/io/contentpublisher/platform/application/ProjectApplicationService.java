package io.contentpublisher.platform.application;

import io.contentpublisher.platform.application.port.ArticleRepository;
import io.contentpublisher.platform.application.port.AuditRecorder;
import io.contentpublisher.platform.application.port.ContentGenerator;
import io.contentpublisher.platform.application.port.ProjectRepository;
import io.contentpublisher.platform.application.port.RepositoryInspector;
import io.contentpublisher.platform.application.port.RepositorySnapshotStore;
import io.contentpublisher.platform.domain.Article;
import io.contentpublisher.platform.domain.ArticleStatus;
import io.contentpublisher.platform.domain.ArticleVersion;
import io.contentpublisher.platform.domain.ActorContext;
import io.contentpublisher.platform.domain.GenerationPolicy;
import io.contentpublisher.platform.domain.Project;
import io.contentpublisher.platform.domain.ProjectStatus;
import io.contentpublisher.platform.domain.RepositorySnapshot;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import java.util.Map;

public final class ProjectApplicationService {
    private final ProjectRepository projects;
    private final ArticleRepository articles;
    private final RepositorySnapshotStore snapshots;
    private final RepositoryInspector repositoryInspector;
    private final ContentGenerator contentGenerator;
    private final AuditRecorder auditRecorder;
    private final Clock clock;

    public ProjectApplicationService(ProjectRepository projects,
                                     ArticleRepository articles,
                                     RepositorySnapshotStore snapshots,
                                     RepositoryInspector repositoryInspector,
                                     ContentGenerator contentGenerator,
                                     AuditRecorder auditRecorder,
                                     Clock clock) {
        this.projects = projects;
        this.articles = articles;
        this.snapshots = snapshots;
        this.repositoryInspector = repositoryInspector;
        this.contentGenerator = contentGenerator;
        this.auditRecorder = auditRecorder;
        this.clock = clock;
    }

    public Project importProject(ActorContext actor, String gitUrl, String branch) {
        Instant now = clock.instant();
        Project existing = projects.findByGitUrl(actor.tenantId(), gitUrl).orElse(null);
        UUID id = existing == null ? UUID.randomUUID() : existing.id();
        Instant createdAt = existing == null ? now : existing.createdAt();
        projects.save(new Project(id, actor.tenantId(), gitUrl, repositoryName(gitUrl), null, branch, null,
                java.util.List.of(), null, ProjectStatus.ANALYZING, actor.subject(), actor.subject(), createdAt, now));
        try {
            RepositorySnapshot snapshot = repositoryInspector.inspect(gitUrl, branch);
            snapshots.save(actor.tenantId(), id, snapshot);
            Project saved = projects.save(new Project(id, actor.tenantId(), gitUrl, snapshot.name(), snapshot.description(),
                    snapshot.defaultBranch(), snapshot.revision(), snapshot.languages(), snapshot.license(),
                    ProjectStatus.READY, existing == null ? actor.subject() : existing.createdBy(), actor.subject(),
                    createdAt, clock.instant()));
            auditRecorder.record(actor, "PROJECT_IMPORTED", "PROJECT", id,
                    Map.of("gitHost", java.net.URI.create(gitUrl).getHost(), "revision", snapshot.revision()));
            return saved;
        } catch (RuntimeException exception) {
            projects.save(new Project(id, actor.tenantId(), gitUrl, repositoryName(gitUrl), null, branch, null,
                    java.util.List.of(), null, ProjectStatus.FAILED,
                    existing == null ? actor.subject() : existing.createdBy(), actor.subject(), createdAt, clock.instant()));
            auditRecorder.record(actor, "PROJECT_IMPORT_FAILED", "PROJECT", id,
                    Map.of("errorType", exception.getClass().getSimpleName()));
            throw exception;
        }
    }

    public Project getProject(ActorContext actor, UUID id) {
        return projects.findProjectById(actor.tenantId(), id)
                .orElseThrow(() -> new ApplicationException("PROJECT_NOT_FOUND", "项目不存在"));
    }

    public Article generateArticle(ActorContext actor, UUID projectId, GenerationPolicy policy) {
        return generateArticle(actor, projectId, policy, null);
    }

    public Article generateArticle(ActorContext actor, UUID projectId, GenerationPolicy policy, UUID generationJobId) {
        if (generationJobId != null) {
            Article existing = articles.findByGenerationJobId(actor.tenantId(), generationJobId).orElse(null);
            if (existing != null) return existing;
        }
        Project project = getProject(actor, projectId);
        if (project.status() != ProjectStatus.READY) {
            throw new ApplicationException("PROJECT_NOT_READY", "项目尚未完成仓库分析");
        }
        RepositorySnapshot snapshot = snapshots.findByProjectId(actor.tenantId(), projectId)
                .orElseThrow(() -> new ApplicationException("SNAPSHOT_NOT_FOUND", "项目仓库快照不存在，请重新导入"));
        ContentGenerator.GeneratedContent generated = contentGenerator.generate(snapshot, policy);
        Instant now = clock.instant();
        Article article = new Article(UUID.randomUUID(), actor.tenantId(), projectId, generationJobId,
                generated.title(), generated.summary(),
                generated.markdown(), generated.keywords(), policy.language(), snapshot.revision(),
                1, ArticleStatus.DRAFT, actor.subject(), actor.subject(), now, now);
        Article saved = articles.saveWithVersion(article, new ArticleVersion(actor.tenantId(), article.id(), 1,
                article.title(), article.summary(), article.markdown(), article.keywords(), actor.subject(), now));
        auditRecorder.record(actor, "ARTICLE_GENERATED", "ARTICLE", saved.id(),
                Map.of("projectId", projectId.toString(), "sourceRevision", snapshot.revision(),
                        "language", policy.language()));
        return saved;
    }

    public Article getArticle(ActorContext actor, UUID articleId) {
        return articles.findArticleById(actor.tenantId(), articleId)
                .orElseThrow(() -> new ApplicationException("ARTICLE_NOT_FOUND", "文章不存在"));
    }

    private String repositoryName(String gitUrl) {
        String clean = gitUrl.endsWith(".git") ? gitUrl.substring(0, gitUrl.length() - 4) : gitUrl;
        int slash = clean.lastIndexOf('/');
        return slash >= 0 ? clean.substring(slash + 1) : clean;
    }
}
