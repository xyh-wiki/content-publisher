package io.contentpublisher.platform.web.dto;

import io.contentpublisher.platform.domain.Job;

import java.util.List;

public record PublicationBatchResponse(int jobCount, List<JobResponse> jobs) {
    public static PublicationBatchResponse from(List<Job> jobs) {
        return new PublicationBatchResponse(jobs.size(), jobs.stream().map(JobResponse::from).toList());
    }
}
