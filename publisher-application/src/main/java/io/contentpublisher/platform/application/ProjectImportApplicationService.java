package io.contentpublisher.platform.application;

import io.contentpublisher.platform.application.port.AuditRecorder;
import io.contentpublisher.platform.application.port.ProjectRepository;
import io.contentpublisher.platform.application.port.RepositoryInspector;
import io.contentpublisher.platform.application.port.RepositorySnapshotStore;
import io.contentpublisher.platform.domain.ActorContext;
import io.contentpublisher.platform.domain.Project;
import io.contentpublisher.platform.domain.ProjectStatus;
import io.contentpublisher.platform.domain.RepositorySnapshot;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ProjectImportApplicationService {
    private final ProjectRepository projects;
    private final RepositorySnapshotStore snapshots;
    private final RepositoryInspector repositoryInspector;
    private final AuditRecorder auditRecorder;
    private final Clock clock;

    public ProjectImportApplicationService(ProjectRepository projects, RepositorySnapshotStore snapshots,
                                           RepositoryInspector repositoryInspector, AuditRecorder auditRecorder,
                                           Clock clock) {
        this.projects = projects;
        this.snapshots = snapshots;
        this.repositoryInspector = repositoryInspector;
        this.auditRecorder = auditRecorder;
        this.clock = clock;
    }

    public Project importProject(ActorContext actor, String gitUrl, String branch) {
        return importProject(actor, gitUrl, branch, JobProgressReporter.noop());
    }

    public Project importProject(ActorContext actor, String gitUrl, String branch, JobProgressReporter progress) {
        progress.update(20, "准备仓库分析", "正在校验仓库地址并创建项目分析记录");
        Instant now = clock.instant();
        Project existing = projects.findByGitUrl(actor.tenantId(), gitUrl).orElse(null);
        UUID id = existing == null ? UUID.randomUUID() : existing.id();
        Instant createdAt = existing == null ? now : existing.createdAt();
        projects.save(new Project(id, actor.tenantId(), gitUrl, repositoryName(gitUrl), null, branch, null,
                List.of(), null, ProjectStatus.ANALYZING, actor.subject(), actor.subject(), createdAt, now));
        try {
            progress.update(35, "读取仓库内容", "正在拉取代码并读取 README、分支和语言信息");
            RepositorySnapshot snapshot = repositoryInspector.inspect(gitUrl, branch);
            progress.update(72, "保存仓库快照", "仓库读取完成，正在持久化可验证的项目事实");
            snapshots.save(actor.tenantId(), id, snapshot);
            Project saved = projects.save(new Project(id, actor.tenantId(), gitUrl, snapshot.name(), snapshot.description(),
                    snapshot.defaultBranch(), snapshot.revision(), snapshot.languages(), snapshot.license(),
                    ProjectStatus.READY, existing == null ? actor.subject() : existing.createdBy(), actor.subject(),
                    createdAt, clock.instant()));
            auditRecorder.record(actor, "PROJECT_IMPORTED", "PROJECT", id,
                    Map.of("gitHost", java.net.URI.create(gitUrl).getHost(), "revision", snapshot.revision()));
            progress.update(92, "仓库分析完成", "项目元数据和仓库快照已保存");
            return saved;
        } catch (RuntimeException exception) {
            projects.save(new Project(id, actor.tenantId(), gitUrl, repositoryName(gitUrl), null, branch, null,
                    List.of(), null, ProjectStatus.FAILED,
                    existing == null ? actor.subject() : existing.createdBy(), actor.subject(), createdAt, clock.instant()));
            auditRecorder.record(actor, "PROJECT_IMPORT_FAILED", "PROJECT", id,
                    Map.of("errorType", exception.getClass().getSimpleName()));
            throw exception;
        }
    }

    private String repositoryName(String gitUrl) {
        String clean = gitUrl.endsWith(".git") ? gitUrl.substring(0, gitUrl.length() - 4) : gitUrl;
        int slash = clean.lastIndexOf('/');
        return slash >= 0 ? clean.substring(slash + 1) : clean;
    }
}
