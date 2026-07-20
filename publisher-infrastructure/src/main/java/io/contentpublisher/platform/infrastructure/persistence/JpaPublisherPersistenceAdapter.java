package io.contentpublisher.platform.infrastructure.persistence;

import io.contentpublisher.platform.application.ApplicationException;
import io.contentpublisher.platform.application.port.ArticleRepository;
import io.contentpublisher.platform.application.port.AuditRecorder;
import io.contentpublisher.platform.application.port.JobRepository;
import io.contentpublisher.platform.application.port.ProjectRepository;
import io.contentpublisher.platform.application.port.RepositorySnapshotStore;
import io.contentpublisher.platform.domain.Article;
import io.contentpublisher.platform.domain.ArticleStatus;
import io.contentpublisher.platform.domain.ActorContext;
import io.contentpublisher.platform.domain.Job;
import io.contentpublisher.platform.domain.JobPayload;
import io.contentpublisher.platform.domain.JobStatus;
import io.contentpublisher.platform.domain.JobType;
import io.contentpublisher.platform.domain.Project;
import io.contentpublisher.platform.domain.ProjectStatus;
import io.contentpublisher.platform.domain.RepositorySnapshot;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.time.Clock;

@Repository
@Transactional
public class JpaPublisherPersistenceAdapter implements ProjectRepository, ArticleRepository, RepositorySnapshotStore,
        AuditRecorder, JobRepository {
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {};

    private final ProjectJpaRepository projects;
    private final ArticleJpaRepository articles;
    private final SnapshotJpaRepository snapshots;
    private final AuditLogJpaRepository auditLogs;
    private final JobJpaRepository jobs;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public JpaPublisherPersistenceAdapter(ProjectJpaRepository projects, ArticleJpaRepository articles,
                                          SnapshotJpaRepository snapshots, AuditLogJpaRepository auditLogs,
                                          JobJpaRepository jobs, ObjectMapper objectMapper, Clock clock) {
        this.projects = projects;
        this.articles = articles;
        this.snapshots = snapshots;
        this.auditLogs = auditLogs;
        this.jobs = jobs;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    public Project save(Project project) {
        ProjectEntity entity = new ProjectEntity();
        entity.id = project.id(); entity.tenantId = project.tenantId(); entity.gitUrl = project.gitUrl(); entity.name = project.name();
        entity.description = project.description(); entity.defaultBranch = project.defaultBranch();
        entity.revision = project.revision(); entity.languagesJson = write(project.languages());
        entity.license = project.license(); entity.status = project.status().name();
        entity.createdBy = project.createdBy(); entity.updatedBy = project.updatedBy();
        entity.createdAt = project.createdAt(); entity.updatedAt = project.updatedAt();
        return toDomain(projects.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Project> findProjectById(String tenantId, UUID id) {
        return projects.findByTenantIdAndId(tenantId, id).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Project> findByGitUrl(String tenantId, String gitUrl) {
        return projects.findByTenantIdAndGitUrl(tenantId, gitUrl).map(this::toDomain);
    }

    @Override
    public Article save(Article article) {
        ArticleEntity entity = new ArticleEntity();
        entity.id = article.id(); entity.tenantId = article.tenantId(); entity.projectId = article.projectId();
        entity.generationJobId = article.generationJobId(); entity.title = article.title();
        entity.summary = article.summary(); entity.markdown = article.markdown();
        entity.keywordsJson = write(article.keywords()); entity.language = article.language();
        entity.sourceRevision = article.sourceRevision(); entity.status = article.status().name();
        entity.createdBy = article.createdBy(); entity.updatedBy = article.updatedBy();
        entity.createdAt = article.createdAt(); entity.updatedAt = article.updatedAt();
        return toDomain(articles.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Article> findArticleById(String tenantId, UUID id) {
        return articles.findByTenantIdAndId(tenantId, id).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Article> findByGenerationJobId(String tenantId, UUID generationJobId) {
        return articles.findByTenantIdAndGenerationJobId(tenantId, generationJobId).map(this::toDomain);
    }

    @Override
    public void save(String tenantId, UUID projectId, RepositorySnapshot snapshot) {
        SnapshotEntity entity = new SnapshotEntity();
        entity.projectId = projectId; entity.tenantId = tenantId; entity.name = snapshot.name(); entity.description = snapshot.description();
        entity.defaultBranch = snapshot.defaultBranch(); entity.revision = snapshot.revision();
        entity.readme = snapshot.readme(); entity.manifestSummary = snapshot.manifestSummary();
        entity.fileTreeJson = write(snapshot.fileTree()); entity.languagesJson = write(snapshot.languages());
        entity.license = snapshot.license();
        snapshots.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RepositorySnapshot> findByProjectId(String tenantId, UUID projectId) {
        return snapshots.findByTenantIdAndProjectId(tenantId, projectId).map(entity -> new RepositorySnapshot(entity.name, entity.description,
                entity.defaultBranch, entity.revision, entity.readme, entity.manifestSummary,
                read(entity.fileTreeJson), read(entity.languagesJson), entity.license));
    }

    private Project toDomain(ProjectEntity entity) {
        return new Project(entity.id, entity.tenantId, entity.gitUrl, entity.name, entity.description, entity.defaultBranch,
                entity.revision, read(entity.languagesJson), entity.license, ProjectStatus.valueOf(entity.status),
                entity.createdBy, entity.updatedBy,
                entity.createdAt, entity.updatedAt);
    }

    private Article toDomain(ArticleEntity entity) {
        return new Article(entity.id, entity.tenantId, entity.projectId, entity.generationJobId,
                entity.title, entity.summary, entity.markdown,
                read(entity.keywordsJson), entity.language, entity.sourceRevision, ArticleStatus.valueOf(entity.status),
                entity.createdBy, entity.updatedBy,
                entity.createdAt, entity.updatedAt);
    }

    @Override
    public void record(ActorContext actor, String action, String targetType, UUID targetId, Map<String, String> details) {
        AuditLogEntity entity = new AuditLogEntity();
        entity.id = UUID.randomUUID(); entity.tenantId = actor.tenantId(); entity.subject = actor.subject();
        entity.action = action; entity.targetType = targetType; entity.targetId = targetId;
        entity.detailsJson = writeMap(details); entity.occurredAt = clock.instant();
        auditLogs.save(entity);
    }

    @Override
    public Job save(Job job) {
        JobEntity entity = new JobEntity();
        entity.id = job.id(); entity.tenantId = job.tenantId(); entity.actorSubject = job.actorSubject();
        entity.type = job.type(); entity.status = job.status(); entity.payloadJson = writePayload(job.payload());
        entity.idempotencyKey = job.idempotencyKey(); entity.requestHash = job.requestHash();
        entity.attempt = job.attempt(); entity.maxAttempts = job.maxAttempts(); entity.scheduledAt = job.scheduledAt();
        entity.lockedAt = job.lockedAt(); entity.lockOwner = job.lockOwner(); entity.resultResourceId = job.resultResourceId();
        entity.errorCode = job.errorCode(); entity.errorMessage = job.errorMessage();
        entity.createdAt = job.createdAt(); entity.updatedAt = job.updatedAt();
        return toDomain(jobs.save(entity));
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Optional<Job> createIfWithinQuota(Job job, int maxActiveJobs) {
        long active = jobs.countByTenantIdAndStatusIn(job.tenantId(),
                List.of(JobStatus.PENDING, JobStatus.RUNNING, JobStatus.RETRY_WAIT));
        return active >= maxActiveJobs ? Optional.empty() : Optional.of(save(job));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Job> findJobById(String tenantId, UUID jobId) {
        return jobs.findByTenantIdAndId(tenantId, jobId).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Job> findByIdempotencyKey(String tenantId, String idempotencyKey) {
        return jobs.findByTenantIdAndIdempotencyKey(tenantId, idempotencyKey).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public long countActiveJobs(String tenantId) {
        return jobs.countByTenantIdAndStatusIn(tenantId,
                List.of(JobStatus.PENDING, JobStatus.RUNNING, JobStatus.RETRY_WAIT));
    }

    @Override
    public Optional<Job> claimNext(String workerId, java.time.Instant now, java.time.Instant staleBefore) {
        jobs.failExpiredExhaustedLeases(JobStatus.RUNNING, JobStatus.FAILED, staleBefore, now,
                "WORKER_LEASE_EXPIRED", "工作器租约过期且任务已耗尽重试次数");
        List<JobEntity> candidates = jobs.findClaimable(List.of(JobStatus.PENDING, JobStatus.RETRY_WAIT),
                JobStatus.RUNNING, now, staleBefore, PageRequest.of(0, 1));
        if (candidates.isEmpty()) return Optional.empty();
        JobEntity entity = candidates.get(0);
        entity.status = JobStatus.RUNNING; entity.lockOwner = workerId; entity.lockedAt = now;
        entity.attempt += 1; entity.errorCode = null; entity.errorMessage = null; entity.updatedAt = now;
        return Optional.of(toDomain(jobs.save(entity)));
    }

    @Override
    public boolean markSucceeded(UUID jobId, String workerId, UUID resultResourceId, java.time.Instant now) {
        return transition(jobId, workerId, entity -> {
            entity.status = JobStatus.SUCCEEDED; entity.resultResourceId = resultResourceId;
            entity.errorCode = null; entity.errorMessage = null; entity.updatedAt = now;
            entity.lockOwner = null; entity.lockedAt = null;
        });
    }

    @Override
    public boolean markRetryWaiting(UUID jobId, String workerId, java.time.Instant scheduledAt,
                                    String errorCode, String errorMessage, java.time.Instant now) {
        return transition(jobId, workerId, entity -> {
            entity.status = JobStatus.RETRY_WAIT; entity.scheduledAt = scheduledAt;
            entity.errorCode = errorCode; entity.errorMessage = limitedError(errorMessage); entity.updatedAt = now;
            entity.lockOwner = null; entity.lockedAt = null;
        });
    }

    @Override
    public boolean markFailed(UUID jobId, String workerId, String errorCode, String errorMessage, java.time.Instant now) {
        return transition(jobId, workerId, entity -> {
            entity.status = JobStatus.FAILED; entity.errorCode = errorCode;
            entity.errorMessage = limitedError(errorMessage); entity.updatedAt = now;
            entity.lockOwner = null; entity.lockedAt = null;
        });
    }

    private boolean transition(UUID jobId, String workerId, java.util.function.Consumer<JobEntity> action) {
        JobEntity entity = jobs.findById(jobId).orElse(null);
        if (entity == null || entity.status != JobStatus.RUNNING || !workerId.equals(entity.lockOwner)) return false;
        action.accept(entity);
        jobs.save(entity);
        return true;
    }

    private Job toDomain(JobEntity entity) {
        return new Job(entity.id, entity.tenantId, entity.actorSubject, entity.type, entity.status,
                readPayload(entity.type, entity.payloadJson), entity.idempotencyKey, entity.requestHash,
                entity.attempt, entity.maxAttempts, entity.scheduledAt, entity.lockedAt, entity.lockOwner,
                entity.resultResourceId, entity.errorCode, entity.errorMessage, entity.createdAt, entity.updatedAt);
    }

    private String writePayload(JobPayload payload) {
        try { return objectMapper.writeValueAsString(payload); }
        catch (JsonProcessingException exception) { throw new ApplicationException("JOB_SERIALIZATION_FAILED", "任务数据序列化失败", exception); }
    }

    private JobPayload readPayload(JobType type, String payload) {
        try {
            return switch (type) {
                case IMPORT_PROJECT -> objectMapper.readValue(payload, JobPayload.ImportProject.class);
                case GENERATE_ARTICLE -> objectMapper.readValue(payload, JobPayload.GenerateArticle.class);
            };
        } catch (JsonProcessingException exception) {
            throw new ApplicationException("JOB_DESERIALIZATION_FAILED", "任务数据反序列化失败", exception);
        }
    }

    private String limitedError(String message) {
        if (message == null) return null;
        String sanitized = message.replaceAll("(?i)(bearer|token|password|api[_-]?key)\\s*[:=]?\\s*[^\\s,;]+", "$1=***");
        return sanitized.length() <= 2000 ? sanitized : sanitized.substring(0, 2000);
    }

    private String write(List<String> values) {
        try { return objectMapper.writeValueAsString(values); }
        catch (JsonProcessingException exception) { throw new ApplicationException("PERSISTENCE_SERIALIZATION_FAILED", "数据序列化失败", exception); }
    }

    private List<String> read(String value) {
        try { return objectMapper.readValue(value, STRING_LIST); }
        catch (JsonProcessingException exception) { throw new ApplicationException("PERSISTENCE_DESERIALIZATION_FAILED", "数据反序列化失败", exception); }
    }

    private String writeMap(Map<String, String> values) {
        try { return objectMapper.writeValueAsString(values); }
        catch (JsonProcessingException exception) { throw new ApplicationException("AUDIT_SERIALIZATION_FAILED", "审计数据序列化失败", exception); }
    }
}
