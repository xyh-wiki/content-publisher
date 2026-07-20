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
    }
}
