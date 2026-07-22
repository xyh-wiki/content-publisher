package io.contentpublisher.platform.application.port;

import io.contentpublisher.platform.domain.Job;
import io.contentpublisher.platform.application.DeletedRecord;
import io.contentpublisher.platform.application.PagedResult;
import io.contentpublisher.platform.domain.JobStatus;
import io.contentpublisher.platform.domain.JobType;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JobRepository {
    Job save(Job job);
    Optional<Job> createIfWithinQuota(Job job, int maxActiveJobs);
    Optional<List<Job>> createBatchIfWithinQuota(List<Job> jobs, int maxActiveJobs);
    Optional<Job> findJobById(String tenantId, UUID jobId);
    Optional<Job> findDeletedJobById(String tenantId, UUID jobId);
    Optional<Job> findByIdempotencyKey(String tenantId, String idempotencyKey);
    List<Job> findRecentJobs(String tenantId, int limit);
    PagedResult<Job> searchJobs(String tenantId, String query, JobType type, JobStatus status,
                                int page, int pageSize);
    List<Job> findByArticleReference(String tenantId, UUID articleId);
    List<Job> findDeletedByArticleReference(String tenantId, UUID articleId);
    List<DeletedRecord> findDeletedJobs(String tenantId, int limit);
    long countActiveJobs(String tenantId);
    boolean softDeleteJobRecord(String tenantId, UUID jobId, String subject, Instant deletedAt);
    boolean restoreJobRecord(String tenantId, UUID jobId);
    boolean cancelPending(String tenantId, UUID jobId, Instant now);
    Optional<Job> claimNext(String workerId, Instant now, Instant staleBefore);
    boolean updateProgress(UUID jobId, String workerId, int percent, String label, String detail, Instant now);
    boolean markSucceeded(UUID jobId, String workerId, UUID resultResourceId, Instant now);
    boolean markRetryWaiting(UUID jobId, String workerId, Instant scheduledAt, String errorCode, String errorMessage, Instant now);
    boolean markFailed(UUID jobId, String workerId, String errorCode, String errorMessage, Instant now);
}
