package io.contentpublisher.platform.application.port;

import io.contentpublisher.platform.domain.Job;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JobRepository {
    Job save(Job job);
    Optional<Job> createIfWithinQuota(Job job, int maxActiveJobs);
    Optional<List<Job>> createBatchIfWithinQuota(List<Job> jobs, int maxActiveJobs);
    Optional<Job> findJobById(String tenantId, UUID jobId);
    Optional<Job> findByIdempotencyKey(String tenantId, String idempotencyKey);
    List<Job> findRecentJobs(String tenantId, int limit);
    long countActiveJobs(String tenantId);
    Optional<Job> claimNext(String workerId, Instant now, Instant staleBefore);
    boolean updateProgress(UUID jobId, String workerId, int percent, String label, String detail, Instant now);
    boolean markSucceeded(UUID jobId, String workerId, UUID resultResourceId, Instant now);
    boolean markRetryWaiting(UUID jobId, String workerId, Instant scheduledAt, String errorCode, String errorMessage, Instant now);
    boolean markFailed(UUID jobId, String workerId, String errorCode, String errorMessage, Instant now);
}
