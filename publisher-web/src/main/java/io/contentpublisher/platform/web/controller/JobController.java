package io.contentpublisher.platform.web.controller;

import io.contentpublisher.platform.application.JobApplicationService;
import io.contentpublisher.platform.application.RecordManagementApplicationService;
import io.contentpublisher.platform.web.dto.JobResponse;
import io.contentpublisher.platform.web.security.RequestActorProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/jobs")
public class JobController {
    private final JobApplicationService jobs;
    private final RecordManagementApplicationService records;
    private final RequestActorProvider actors;

    public JobController(JobApplicationService jobs, RecordManagementApplicationService records,
                         RequestActorProvider actors) {
        this.jobs = jobs;
        this.records = records;
        this.actors = actors;
    }

    @GetMapping("/{jobId}")
    public JobResponse getJob(@PathVariable UUID jobId) {
        return JobResponse.from(jobs.getJob(actors.currentActor(), jobId));
    }

    @DeleteMapping("/{jobId}")
    public ResponseEntity<Void> deleteJob(@PathVariable UUID jobId) {
        records.deleteJobRecord(actors.currentActor(), jobId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{jobId}/restore")
    public ResponseEntity<Void> restoreJob(@PathVariable UUID jobId) {
        records.restoreJobRecord(actors.currentActor(), jobId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{jobId}/publication-retry")
    public ResponseEntity<JobResponse> retryPublication(
            @PathVariable UUID jobId,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        var retried = jobs.retryFailedPublication(actors.currentActor(), jobId, idempotencyKey);
        return ResponseEntity.accepted().header(HttpHeaders.LOCATION, "/api/v1/jobs/" + retried.id())
                .body(JobResponse.from(retried));
    }

    @PostMapping("/{jobId}/cancel")
    public JobResponse cancel(@PathVariable UUID jobId) {
        return JobResponse.from(jobs.cancelJob(actors.currentActor(), jobId));
    }
}
