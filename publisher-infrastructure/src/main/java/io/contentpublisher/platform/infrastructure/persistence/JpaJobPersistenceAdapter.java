package io.contentpublisher.platform.infrastructure.persistence;

import io.contentpublisher.platform.application.DeletedRecord;
import io.contentpublisher.platform.application.port.JobRepository;
import io.contentpublisher.platform.domain.Job;
import io.contentpublisher.platform.domain.JobPayload;
import io.contentpublisher.platform.domain.JobStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

@Repository
@Transactional
public class JpaJobPersistenceAdapter implements JobRepository {
    private final JobJpaRepository jobs;
    private final PublicationJpaRepository publications;
    private final JpaDomainMapper mapper;

    public JpaJobPersistenceAdapter(JobJpaRepository jobs, PublicationJpaRepository publications,
                                    JpaDomainMapper mapper) {
        this.jobs = jobs;
        this.publications = publications;
        this.mapper = mapper;
    }

    @Override
    public Job save(Job job) {
        JobEntity entity = new JobEntity();
        entity.id = job.id(); entity.tenantId = job.tenantId(); entity.actorSubject = job.actorSubject();
        entity.type = job.type(); entity.status = job.status(); entity.payloadJson = mapper.payloadJson(job.payload());
        entity.idempotencyKey = job.idempotencyKey(); entity.requestHash = job.requestHash();
        entity.attempt = job.attempt(); entity.maxAttempts = job.maxAttempts();
        entity.progressPercent = job.progressPercent(); entity.progressLabel = job.progressLabel();
        entity.progressDetail = job.progressDetail(); entity.batchId = job.batchId();
        entity.scheduledAt = job.scheduledAt(); entity.lockedAt = job.lockedAt();
        entity.lockOwner = job.lockOwner(); entity.resultResourceId = job.resultResourceId();
        entity.errorCode = job.errorCode(); entity.errorMessage = job.errorMessage();
        entity.createdAt = job.createdAt(); entity.updatedAt = job.updatedAt();
        return mapper.job(jobs.save(entity));
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Optional<Job> createIfWithinQuota(Job job, int maxActiveJobs) {
        long active = jobs.countByTenantIdAndDeletedAtIsNullAndStatusIn(job.tenantId(), activeStatuses());
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
        long active = jobs.countByTenantIdAndDeletedAtIsNullAndStatusIn(tenantId, activeStatuses());
        if (active + batch.size() > maxActiveJobs) return Optional.empty();
        return Optional.of(batch.stream().map(this::save).toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Job> findJobById(String tenantId, UUID jobId) {
        return jobs.findByTenantIdAndIdAndDeletedAtIsNull(tenantId, jobId).map(mapper::job);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Job> findDeletedJobById(String tenantId, UUID jobId) {
        return jobs.findByTenantIdAndIdAndDeletedAtIsNotNull(tenantId, jobId).map(mapper::job);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Job> findByIdempotencyKey(String tenantId, String idempotencyKey) {
        return jobs.findByTenantIdAndIdempotencyKey(tenantId, idempotencyKey).map(mapper::job);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Job> findRecentJobs(String tenantId, int limit) {
        return jobs.findByTenantIdAndDeletedAtIsNullOrderByUpdatedAtDesc(tenantId, PageRequest.of(0, limit))
                .stream().map(mapper::job).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Job> findByArticleReference(String tenantId, UUID articleId) {
        return jobs.findAllByTenantIdAndDeletedAtIsNull(tenantId).stream().map(mapper::job)
                .filter(job -> articleId.equals(job.resultResourceId())
                        || job.payload() instanceof JobPayload.PublishArticle publication
                        && articleId.equals(publication.articleId()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Job> findDeletedByArticleReference(String tenantId, UUID articleId) {
        return jobs.findAllByTenantIdAndDeletedAtIsNotNull(tenantId).stream().map(mapper::job)
                .filter(job -> articleId.equals(job.resultResourceId())
                        || job.payload() instanceof JobPayload.PublishArticle publication
                        && articleId.equals(publication.articleId()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeletedRecord> findDeletedJobs(String tenantId, int limit) {
        return jobs.findByTenantIdAndDeletedAtIsNotNullOrderByDeletedAtDesc(tenantId, PageRequest.of(0, limit))
                .stream().map(entity -> new DeletedRecord(entity.id, "JOB", entity.type.name(), entity.status.name(),
                        entity.deletedAt, entity.deletedBy)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countActiveJobs(String tenantId) {
        return jobs.countByTenantIdAndDeletedAtIsNullAndStatusIn(tenantId, activeStatuses());
    }

    @Override
    public boolean softDeleteJobRecord(String tenantId, UUID jobId, String subject, Instant deletedAt) {
        JobEntity job = jobs.findByTenantIdAndIdAndDeletedAtIsNull(tenantId, jobId).orElse(null);
        if (job == null) return false;
        publications.findByTenantIdAndPublicationJobId(tenantId, jobId).ifPresent(publication -> {
            publication.deletedAt = deletedAt; publication.deletedBy = subject;
        });
        job.deletedAt = deletedAt; job.deletedBy = subject;
        jobs.save(job);
        return true;
    }

    @Override
    public boolean restoreJobRecord(String tenantId, UUID jobId) {
        JobEntity job = jobs.findByTenantIdAndIdAndDeletedAtIsNotNull(tenantId, jobId).orElse(null);
        if (job == null) return false;
        publications.findByTenantIdAndPublicationJobId(tenantId, jobId).ifPresent(publication -> {
            publication.deletedAt = null; publication.deletedBy = null;
        });
        job.deletedAt = null; job.deletedBy = null;
        jobs.save(job);
        return true;
    }

    @Override
    public Optional<Job> claimNext(String workerId, Instant now, Instant staleBefore) {
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
        return Optional.of(mapper.job(jobs.save(entity)));
    }

    @Override
    public boolean updateProgress(UUID jobId, String workerId, int percent, String label, String detail, Instant now) {
        if (percent < 0 || percent > 99) throw new IllegalArgumentException("运行中任务进度必须在 0 到 99 之间");
        return transition(jobId, workerId, entity -> {
            entity.progressPercent = Math.max(entity.progressPercent, percent);
            entity.progressLabel = limited(label, 100); entity.progressDetail = limited(detail, 500);
            entity.updatedAt = now;
        });
    }

    @Override
    public boolean markSucceeded(UUID jobId, String workerId, UUID resultResourceId, Instant now) {
        return transition(jobId, workerId, entity -> {
            entity.status = JobStatus.SUCCEEDED; entity.resultResourceId = resultResourceId;
            entity.errorCode = null; entity.errorMessage = null; entity.updatedAt = now;
            entity.progressPercent = 100; entity.progressLabel = "执行完成";
            entity.progressDetail = "任务结果已经保存，可以继续下一步操作";
            entity.lockOwner = null; entity.lockedAt = null;
        });
    }

    @Override
    public boolean markRetryWaiting(UUID jobId, String workerId, Instant scheduledAt,
                                    String errorCode, String errorMessage, Instant now) {
        return transition(jobId, workerId, entity -> {
            entity.status = JobStatus.RETRY_WAIT; entity.scheduledAt = scheduledAt;
            entity.errorCode = errorCode; entity.errorMessage = limitedError(errorMessage); entity.updatedAt = now;
            entity.progressPercent = 15; entity.progressLabel = "等待重试";
            entity.progressDetail = "本次执行未完成，系统将按照重试策略再次处理";
            entity.lockOwner = null; entity.lockedAt = null;
        });
    }

    @Override
    public boolean markFailed(UUID jobId, String workerId, String errorCode, String errorMessage, Instant now) {
        return transition(jobId, workerId, entity -> {
            entity.status = JobStatus.FAILED; entity.errorCode = errorCode;
            entity.errorMessage = limitedError(errorMessage); entity.updatedAt = now;
            entity.progressPercent = 100; entity.progressLabel = "执行失败";
            entity.progressDetail = "任务已经停止，请根据异常信息调整后重试";
            entity.lockOwner = null; entity.lockedAt = null;
        });
    }

    private boolean transition(UUID jobId, String workerId, Consumer<JobEntity> action) {
        JobEntity entity = jobs.findById(jobId).orElse(null);
        if (entity == null || entity.status != JobStatus.RUNNING || !workerId.equals(entity.lockOwner)) return false;
        action.accept(entity);
        jobs.save(entity);
        return true;
    }

    private List<JobStatus> activeStatuses() {
        return List.of(JobStatus.PENDING, JobStatus.RUNNING, JobStatus.RETRY_WAIT);
    }

    private String limitedError(String message) {
        if (message == null) return null;
        String sanitized = message.replaceAll(
                "(?i)(bearer|token|password|api[_-]?key)\\s*[:=]?\\s*[^\\s,;]+", "$1=***");
        return sanitized.length() <= 2000 ? sanitized : sanitized.substring(0, 2000);
    }

    private String limited(String value, int maxLength) {
        String normalized = value == null ? "" : value.trim();
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
    }
}
