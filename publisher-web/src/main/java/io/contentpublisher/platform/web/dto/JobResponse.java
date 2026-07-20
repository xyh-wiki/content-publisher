package io.contentpublisher.platform.web.dto;

import io.contentpublisher.platform.domain.Job;

import java.time.Instant;
import java.util.UUID;

public record JobResponse(UUID id, String type, String status, int attempt, int maxAttempts,
                          Instant scheduledAt, UUID resultResourceId, String resultResourceType,
                          String errorCode, String errorMessage, Instant createdAt, Instant updatedAt) {
    public static JobResponse from(Job job) {
        String resourceType = switch (job.type()) {
            case IMPORT_PROJECT -> "PROJECT";
            case GENERATE_ARTICLE -> "ARTICLE";
            case PUBLISH_ARTICLE -> "PUBLICATION";
        };
        return new JobResponse(job.id(), job.type().name(), job.status().name(), job.attempt(), job.maxAttempts(),
                job.scheduledAt(), job.resultResourceId(), resourceType, job.errorCode(), job.errorMessage(),
                job.createdAt(), job.updatedAt());
    }
}
