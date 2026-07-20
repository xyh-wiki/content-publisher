package io.contentpublisher.platform.infrastructure.persistence;

import io.contentpublisher.platform.application.ApplicationException;
import io.contentpublisher.platform.application.port.ArticleRepository;
import io.contentpublisher.platform.application.port.AiProviderSettingsRepository;
import io.contentpublisher.platform.application.port.AuditRecorder;
import io.contentpublisher.platform.application.port.JobRepository;
import io.contentpublisher.platform.application.port.ManualPublicationRepository;
import io.contentpublisher.platform.application.port.ChannelAccountRepository;
import io.contentpublisher.platform.application.port.PublicationRepository;
import io.contentpublisher.platform.application.port.ProjectRepository;
import io.contentpublisher.platform.application.port.RepositorySnapshotStore;
import io.contentpublisher.platform.domain.Article;
import io.contentpublisher.platform.domain.AiProviderSettings;
import io.contentpublisher.platform.domain.ArticleStatus;
import io.contentpublisher.platform.domain.ArticleVersion;
import io.contentpublisher.platform.domain.ArticleSourceType;
import io.contentpublisher.platform.domain.ContentOrigin;
import io.contentpublisher.platform.domain.ActorContext;
import io.contentpublisher.platform.domain.Job;
import io.contentpublisher.platform.domain.ManualPublication;
import io.contentpublisher.platform.domain.JobPayload;
import io.contentpublisher.platform.domain.JobStatus;
import io.contentpublisher.platform.domain.JobType;
import io.contentpublisher.platform.domain.Project;
import io.contentpublisher.platform.domain.ProjectStatus;
import io.contentpublisher.platform.domain.RepositorySnapshot;
import io.contentpublisher.platform.domain.ChannelAccount;
import io.contentpublisher.platform.domain.Publication;
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
import java.math.BigDecimal;

@Repository
@Transactional
public class JpaPublisherPersistenceAdapter implements ProjectRepository, ArticleRepository, RepositorySnapshotStore,
        AuditRecorder, JobRepository, ChannelAccountRepository, PublicationRepository, ManualPublicationRepository,
        AiProviderSettingsRepository {
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {};

    private final ProjectJpaRepository projects;
    private final ArticleJpaRepository articles;
    private final ArticleVersionJpaRepository articleVersions;
    private final SnapshotJpaRepository snapshots;
    private final AuditLogJpaRepository auditLogs;
    private final AiProviderSettingsJpaRepository aiProviderSettings;
    private final JobJpaRepository jobs;
    private final ChannelAccountJpaRepository channelAccounts;
    private final PublicationJpaRepository publications;
    private final ManualPublicationJpaRepository manualPublications;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public JpaPublisherPersistenceAdapter(ProjectJpaRepository projects, ArticleJpaRepository articles,
                                          ArticleVersionJpaRepository articleVersions,
                                          SnapshotJpaRepository snapshots, AuditLogJpaRepository auditLogs,
                                          AiProviderSettingsJpaRepository aiProviderSettings,
                                          JobJpaRepository jobs, ChannelAccountJpaRepository channelAccounts,
                                          PublicationJpaRepository publications,
                                          ManualPublicationJpaRepository manualPublications,
                                          ObjectMapper objectMapper, Clock clock) {
        this.projects = projects;
        this.articles = articles;
        this.articleVersions = articleVersions;
        this.snapshots = snapshots;
        this.auditLogs = auditLogs;
        this.aiProviderSettings = aiProviderSettings;
        this.jobs = jobs;
        this.channelAccounts = channelAccounts;
        this.publications = publications;
        this.manualPublications = manualPublications;
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
    @Transactional(readOnly = true)
    public List<Project> findRecentProjects(String tenantId, int limit) {
        return projects.findByTenantIdOrderByUpdatedAtDesc(tenantId, PageRequest.of(0, limit)).stream()
                .map(this::toDomain).toList();
    }

    @Override
    public Article save(Article article) {
        ArticleEntity entity = new ArticleEntity();
        entity.id = article.id(); entity.tenantId = article.tenantId(); entity.projectId = article.projectId();
        entity.sourceType = article.sourceType().name(); entity.sourceTitle = article.origin().title();
        entity.sourceUrl = article.origin().sourceUrl();
        entity.sourceDescription = article.origin().description(); entity.targetAudience = article.origin().audience();
        entity.articleType = article.origin().articleType(); entity.knowledgeLevel = article.origin().knowledgeLevel();
        entity.sourceKeywordsJson = write(article.origin().requestedKeywords());
        entity.generationJobId = article.generationJobId(); entity.title = article.title();
        entity.summary = article.summary(); entity.markdown = article.markdown(); entity.tagsJson = write(article.tags());
        entity.keywordsJson = write(article.keywords()); entity.language = article.language();
        entity.sourceRevision = article.sourceRevision(); entity.currentVersion = article.currentVersion();
        entity.status = article.status().name();
        entity.createdBy = article.createdBy(); entity.updatedBy = article.updatedBy();
        entity.createdAt = article.createdAt(); entity.updatedAt = article.updatedAt();
        return toDomain(articles.save(entity));
    }

    @Override
    public Article saveWithVersion(Article article, ArticleVersion version) {
        Article saved = save(article);
        ArticleVersionEntity entity = new ArticleVersionEntity();
        entity.id = new ArticleVersionKey(version.articleId(), version.versionNumber());
        entity.tenantId = version.tenantId(); entity.title = version.title(); entity.summary = version.summary();
        entity.markdown = version.markdown(); entity.tagsJson = write(version.tags());
        entity.keywordsJson = write(version.keywords());
        entity.createdBy = version.createdBy(); entity.createdAt = version.createdAt();
        articleVersions.save(entity);
        return saved;
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
    @Transactional(readOnly = true)
    public List<Article> findRecentArticles(String tenantId, int limit) {
        return articles.findByTenantIdOrderByUpdatedAtDesc(tenantId, PageRequest.of(0, limit)).stream()
                .map(this::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Article> findRecentByProjectId(String tenantId, UUID projectId, int limit) {
        return articles.findByTenantIdAndProjectIdOrderByUpdatedAtDesc(
                tenantId, projectId, PageRequest.of(0, limit)).stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ArticleVersion> findVersions(String tenantId, UUID articleId) {
        return articleVersions.findVersions(tenantId, articleId).stream().map(entity ->
                new ArticleVersion(entity.tenantId, entity.id.articleId, entity.id.versionNumber,
                        entity.title, entity.summary, entity.markdown, read(entity.tagsJson), read(entity.keywordsJson),
                        entity.createdBy, entity.createdAt)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AiProviderSettings> findByTenantId(String tenantId) {
        return aiProviderSettings.findById(tenantId).map(this::toDomain);
    }

    @Override
    public AiProviderSettings save(AiProviderSettings settings) {
        AiProviderSettingsEntity entity = new AiProviderSettingsEntity();
        entity.tenantId = settings.tenantId(); entity.baseUrl = settings.baseUrl();
        entity.encryptedApiKey = settings.encryptedApiKey();
        entity.apiKeyFingerprint = settings.apiKeyFingerprint(); entity.model = settings.model();
        entity.timeoutSeconds = settings.timeoutSeconds(); entity.temperature = BigDecimal.valueOf(settings.temperature());
        entity.enabled = settings.enabled(); entity.settingsVersion = settings.version();
        entity.createdBy = settings.createdBy(); entity.updatedBy = settings.updatedBy();
        entity.createdAt = settings.createdAt(); entity.updatedAt = settings.updatedAt();
        return toDomain(aiProviderSettings.save(entity));
    }

    @Override
    public Optional<AiProviderSettings> updateIfVersionMatches(AiProviderSettings settings, int expectedVersion) {
        int updated = aiProviderSettings.updateIfVersionMatches(settings.tenantId(), settings.baseUrl(),
                settings.encryptedApiKey(), settings.apiKeyFingerprint(), settings.model(), settings.timeoutSeconds(),
                BigDecimal.valueOf(settings.temperature()), settings.enabled(), expectedVersion, settings.version(), settings.updatedBy(),
                settings.updatedAt());
        if (updated == 0) return Optional.empty();
        return aiProviderSettings.findById(settings.tenantId()).map(this::toDomain);
    }

    @Override
    public ChannelAccount save(ChannelAccount account) {
        ChannelAccountEntity entity = new ChannelAccountEntity();
        entity.id = account.id(); entity.tenantId = account.tenantId(); entity.type = account.type();
        entity.displayName = account.displayName(); entity.baseUrl = account.baseUrl();
        entity.encryptedCredentials = account.encryptedCredentials(); entity.idempotencyKey = account.idempotencyKey();
        entity.requestHash = account.requestHash(); entity.credentialFingerprint = account.credentialFingerprint();
        entity.accountVersion = account.version(); entity.status = account.status();
        entity.createdBy = account.createdBy(); entity.updatedBy = account.updatedBy();
        entity.createdAt = account.createdAt(); entity.updatedAt = account.updatedAt();
        return toDomain(channelAccounts.save(entity));
    }

    @Override
    public Optional<ChannelAccount> updateIfVersionMatches(ChannelAccount account, int expectedVersion) {
        int updated = channelAccounts.updateIfVersionMatches(account.tenantId(), account.id(),
                account.encryptedCredentials(), account.credentialFingerprint(), account.status(), expectedVersion,
                account.version(), account.updatedBy(), account.updatedAt());
        if (updated == 0) return Optional.empty();
        return channelAccounts.findByTenantIdAndId(account.tenantId(), account.id()).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ChannelAccount> findChannelAccountById(String tenantId, UUID id) {
        return channelAccounts.findByTenantIdAndId(tenantId, id).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ChannelAccount> findChannelAccountByIdempotencyKey(String tenantId, String idempotencyKey) {
        return channelAccounts.findByTenantIdAndIdempotencyKey(tenantId, idempotencyKey).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChannelAccount> findAll(String tenantId) {
        return channelAccounts.findAllByTenantIdOrderByCreatedAtDesc(tenantId).stream().map(this::toDomain).toList();
    }

    @Override
    public Publication save(Publication publication) {
        PublicationEntity entity = new PublicationEntity();
        entity.id = publication.id(); entity.tenantId = publication.tenantId();
        entity.articleId = publication.articleId(); entity.channelAccountId = publication.channelAccountId();
        entity.publicationJobId = publication.publicationJobId(); entity.channelType = publication.channelType();
        entity.canonicalUrl = publication.canonicalUrl(); entity.status = publication.status(); entity.externalId = publication.externalId();
        entity.externalUrl = publication.externalUrl(); entity.errorCode = publication.errorCode();
        entity.errorMessage = publication.errorMessage(); entity.publishedAt = publication.publishedAt();
        entity.createdAt = publication.createdAt(); entity.updatedAt = publication.updatedAt();
        return toDomain(publications.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Publication> findPublicationById(String tenantId, UUID id) {
        return publications.findByTenantIdAndId(tenantId, id).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Publication> findByPublicationJobId(String tenantId, UUID jobId) {
        return publications.findByTenantIdAndPublicationJobId(tenantId, jobId).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Publication> findApiByArticle(String tenantId, UUID articleId) {
        return publications.findByTenantIdAndArticleIdOrderByUpdatedAtDesc(tenantId, articleId).stream()
                .map(this::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Publication> findApiByArticles(String tenantId, List<UUID> articleIds) {
        return publications.findByTenantIdAndArticleIdInOrderByUpdatedAtDesc(tenantId, articleIds).stream()
                .map(this::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Publication> findRecentApi(String tenantId, int limit) {
        return publications.findByTenantIdOrderByUpdatedAtDesc(tenantId, PageRequest.of(0, limit)).stream()
                .map(this::toDomain).toList();
    }

    @Override
    public ManualPublication save(ManualPublication publication) {
        ManualPublicationEntity entity = new ManualPublicationEntity();
        entity.id = publication.id(); entity.tenantId = publication.tenantId();
        entity.articleId = publication.articleId(); entity.channelType = publication.channelType();
        entity.contentFormat = publication.contentFormat(); entity.adaptedTitle = publication.adaptedTitle();
        entity.adaptedContent = publication.adaptedContent(); entity.externalUrl = publication.externalUrl();
        entity.publishedBy = publication.publishedBy(); entity.publishedAt = publication.publishedAt();
        return toDomain(manualPublications.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ManualPublication> findByArticle(String tenantId, UUID articleId) {
        return manualPublications.findByTenantIdAndArticleIdOrderByPublishedAtDesc(tenantId, articleId).stream()
                .map(this::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ManualPublication> findByArticles(String tenantId, List<UUID> articleIds) {
        return manualPublications.findByTenantIdAndArticleIdInOrderByPublishedAtDesc(tenantId, articleIds).stream()
                .map(this::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ManualPublication> findRecent(String tenantId, int limit) {
        return manualPublications.findByTenantIdOrderByPublishedAtDesc(tenantId,
                org.springframework.data.domain.PageRequest.of(0, limit)).stream().map(this::toDomain).toList();
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
        ArticleSourceType sourceType = ArticleSourceType.valueOf(entity.sourceType);
        ContentOrigin origin = sourceType == ArticleSourceType.GIT ? ContentOrigin.git(entity.projectId)
                : new ContentOrigin(sourceType, null, entity.sourceUrl, entity.sourceTitle, entity.sourceDescription,
                        entity.targetAudience, entity.articleType, entity.knowledgeLevel, read(entity.sourceKeywordsJson));
        return new Article(entity.id, entity.tenantId, origin, entity.generationJobId,
                entity.title, entity.summary, entity.markdown, read(entity.tagsJson),
                read(entity.keywordsJson), entity.language, entity.sourceRevision, entity.currentVersion,
                ArticleStatus.valueOf(entity.status),
                entity.createdBy, entity.updatedBy,
                entity.createdAt, entity.updatedAt);
    }

    private AiProviderSettings toDomain(AiProviderSettingsEntity entity) {
        return new AiProviderSettings(entity.tenantId, entity.baseUrl, entity.encryptedApiKey,
                entity.apiKeyFingerprint, entity.model, entity.timeoutSeconds, entity.temperature.doubleValue(), entity.enabled,
                entity.settingsVersion, entity.createdBy, entity.updatedBy, entity.createdAt, entity.updatedAt);
    }

    private ChannelAccount toDomain(ChannelAccountEntity entity) {
        return new ChannelAccount(entity.id, entity.tenantId, entity.type, entity.displayName, entity.baseUrl,
                entity.encryptedCredentials, entity.idempotencyKey, entity.requestHash,
                entity.credentialFingerprint, entity.accountVersion, entity.status, entity.createdBy, entity.updatedBy,
                entity.createdAt, entity.updatedAt);
    }

    private Publication toDomain(PublicationEntity entity) {
        return new Publication(entity.id, entity.tenantId, entity.articleId, entity.channelAccountId,
                entity.publicationJobId, entity.channelType, entity.canonicalUrl, entity.status, entity.externalId, entity.externalUrl,
                entity.errorCode, entity.errorMessage, entity.publishedAt, entity.createdAt, entity.updatedAt);
    }

    private ManualPublication toDomain(ManualPublicationEntity entity) {
        return new ManualPublication(entity.id, entity.tenantId, entity.articleId, entity.channelType,
                entity.contentFormat, entity.adaptedTitle, entity.adaptedContent, entity.externalUrl,
                entity.publishedBy, entity.publishedAt);
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
        entity.attempt = job.attempt(); entity.maxAttempts = job.maxAttempts();
        entity.progressPercent = job.progressPercent(); entity.progressLabel = job.progressLabel();
        entity.progressDetail = job.progressDetail(); entity.batchId = job.batchId();
        entity.scheduledAt = job.scheduledAt();
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
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Optional<List<Job>> createBatchIfWithinQuota(List<Job> batch, int maxActiveJobs) {
        if (batch.isEmpty()) return Optional.of(List.of());
        String tenantId = batch.get(0).tenantId();
        if (batch.stream().anyMatch(job -> !tenantId.equals(job.tenantId()))) {
            throw new IllegalArgumentException("批量任务必须属于同一租户");
        }
        long active = jobs.countByTenantIdAndStatusIn(tenantId,
                List.of(JobStatus.PENDING, JobStatus.RUNNING, JobStatus.RETRY_WAIT));
        if (active + batch.size() > maxActiveJobs) return Optional.empty();
        return Optional.of(batch.stream().map(this::save).toList());
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
    public List<Job> findRecentJobs(String tenantId, int limit) {
        return jobs.findByTenantIdOrderByUpdatedAtDesc(tenantId, PageRequest.of(0, limit)).stream()
                .map(this::toDomain).toList();
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
        entity.attempt += 1; entity.errorCode = null; entity.errorMessage = null;
        entity.progressPercent = 12; entity.progressLabel = "任务已领取";
        entity.progressDetail = "后台工作器已领取任务，准备执行实际处理步骤"; entity.updatedAt = now;
        return Optional.of(toDomain(jobs.save(entity)));
    }

    @Override
    public boolean updateProgress(UUID jobId, String workerId, int percent, String label, String detail,
                                  java.time.Instant now) {
        if (percent < 0 || percent > 99) throw new IllegalArgumentException("运行中任务进度必须在 0 到 99 之间");
        return transition(jobId, workerId, entity -> {
            entity.progressPercent = Math.max(entity.progressPercent, percent);
            entity.progressLabel = limited(label, 100);
            entity.progressDetail = limited(detail, 500);
            entity.updatedAt = now;
        });
    }

    @Override
    public boolean markSucceeded(UUID jobId, String workerId, UUID resultResourceId, java.time.Instant now) {
        return transition(jobId, workerId, entity -> {
            entity.status = JobStatus.SUCCEEDED; entity.resultResourceId = resultResourceId;
            entity.errorCode = null; entity.errorMessage = null; entity.updatedAt = now;
            entity.progressPercent = 100; entity.progressLabel = "执行完成";
            entity.progressDetail = "任务结果已经保存，可以继续下一步操作";
            entity.lockOwner = null; entity.lockedAt = null;
        });
    }

    @Override
    public boolean markRetryWaiting(UUID jobId, String workerId, java.time.Instant scheduledAt,
                                    String errorCode, String errorMessage, java.time.Instant now) {
        return transition(jobId, workerId, entity -> {
            entity.status = JobStatus.RETRY_WAIT; entity.scheduledAt = scheduledAt;
            entity.errorCode = errorCode; entity.errorMessage = limitedError(errorMessage); entity.updatedAt = now;
            entity.progressPercent = 15; entity.progressLabel = "等待重试";
            entity.progressDetail = "本次执行未完成，系统将按照重试策略再次处理";
            entity.lockOwner = null; entity.lockedAt = null;
        });
    }

    @Override
    public boolean markFailed(UUID jobId, String workerId, String errorCode, String errorMessage, java.time.Instant now) {
        return transition(jobId, workerId, entity -> {
            entity.status = JobStatus.FAILED; entity.errorCode = errorCode;
            entity.errorMessage = limitedError(errorMessage); entity.updatedAt = now;
            entity.progressPercent = 100; entity.progressLabel = "执行失败";
            entity.progressDetail = "任务已经停止，请根据异常信息调整后重试";
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
                entity.attempt, entity.maxAttempts, entity.progressPercent, entity.progressLabel,
                entity.progressDetail, entity.batchId, entity.scheduledAt, entity.lockedAt, entity.lockOwner,
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
                case GENERATE_TOPIC_ARTICLE -> objectMapper.readValue(payload, JobPayload.GenerateTopicArticle.class);
                case GENERATE_WEBSITE_ARTICLE -> objectMapper.readValue(payload, JobPayload.GenerateWebsiteArticle.class);
                case PUBLISH_ARTICLE -> objectMapper.readValue(payload, JobPayload.PublishArticle.class);
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

    private String limited(String value, int maxLength) {
        String normalized = value == null ? "" : value.trim();
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
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
