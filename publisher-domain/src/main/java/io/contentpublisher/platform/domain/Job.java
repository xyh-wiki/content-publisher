package io.contentpublisher.platform.domain;

import java.time.Instant;
import java.util.UUID;

public record Job(
        UUID id,
        String tenantId,
        String actorSubject,
        JobType type,
        JobStatus status,
        JobPayload payload,
        String idempotencyKey,
        String requestHash,
        int attempt,
        int maxAttempts,
        int progressPercent,
        String progressLabel,
        String progressDetail,
        UUID batchId,
        Instant scheduledAt,
        Instant lockedAt,
        String lockOwner,
        UUID resultResourceId,
        String errorCode,
        String errorMessage,
        Instant createdAt,
        Instant updatedAt) {

    public Job {
        if (attempt < 0 || maxAttempts < 1 || attempt > maxAttempts) {
            throw new IllegalArgumentException("任务重试次数无效");
        }
        if (progressPercent < 0 || progressPercent > 100) {
            throw new IllegalArgumentException("任务进度必须在 0 到 100 之间");
        }
        progressLabel = progressLabel == null ? "" : progressLabel.trim();
        progressDetail = progressDetail == null ? "" : progressDetail.trim();
        if (progressLabel.length() > 100 || progressDetail.length() > 500) {
            throw new IllegalArgumentException("任务进度说明过长");
        }
    }
}
