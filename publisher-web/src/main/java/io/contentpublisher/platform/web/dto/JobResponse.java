package io.contentpublisher.platform.web.dto;

import io.contentpublisher.platform.domain.Job;

import java.time.Instant;
import java.util.UUID;

public record JobResponse(UUID id, String type, String status, int attempt, int maxAttempts,
                          int progressPercent, String progressLabel, String progressDetail,
                          UUID batchId,
                          Instant scheduledAt, UUID resultResourceId, String resultResourceType,
                          String errorCode, String errorMessage, Instant createdAt, Instant updatedAt) {
    public static JobResponse from(Job job) {
        JobProgressView progress = JobProgressView.from(job);
        String resourceType = switch (job.type()) {
            case IMPORT_PROJECT -> "PROJECT";
            case GENERATE_ARTICLE, GENERATE_TOPIC_ARTICLE, GENERATE_WEBSITE_ARTICLE -> "ARTICLE";
            case PUBLISH_ARTICLE -> "PUBLICATION";
        };
        return new JobResponse(job.id(), job.type().name(), job.status().name(), job.attempt(), job.maxAttempts(),
                progress.percent(), progress.label(), progress.detail(),
                job.batchId(), job.scheduledAt(), job.resultResourceId(), resourceType, job.errorCode(), job.errorMessage(),
                job.createdAt(), job.updatedAt());
    }
}
