package io.contentpublisher.platform.web.controller;

import io.contentpublisher.platform.application.ProjectApplicationService;
import io.contentpublisher.platform.application.JobApplicationService;
import io.contentpublisher.platform.domain.GenerationPolicy;
import io.contentpublisher.platform.web.dto.GenerateArticleRequest;
import io.contentpublisher.platform.web.dto.ImportProjectRequest;
import io.contentpublisher.platform.web.dto.ProjectResponse;
import io.contentpublisher.platform.web.dto.JobResponse;
import io.contentpublisher.platform.web.security.RequestActorProvider;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {
    private final ProjectApplicationService service;
    private final JobApplicationService jobs;
    private final RequestActorProvider actors;

    public ProjectController(ProjectApplicationService service, JobApplicationService jobs, RequestActorProvider actors) {
        this.service = service;
        this.jobs = jobs;
        this.actors = actors;
    }

    @PostMapping("/imports")
    public ResponseEntity<JobResponse> importProject(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody ImportProjectRequest request) {
        var job = jobs.submitImport(actors.currentActor(), request.gitUrl().trim(), trimToNull(request.branch()), idempotencyKey);
        return ResponseEntity.accepted()
                .header(HttpHeaders.LOCATION, "/api/v1/jobs/" + job.id())
                .body(JobResponse.from(job));
    }

    @GetMapping("/{projectId}")
    public ProjectResponse getProject(@PathVariable UUID projectId) {
        return ProjectResponse.from(service.getProject(actors.currentActor(), projectId));
    }

    @PostMapping("/{projectId}/articles")
    public ResponseEntity<JobResponse> generateArticle(
                                           @PathVariable UUID projectId,
                                           @RequestHeader("Idempotency-Key") String idempotencyKey,
                                           @Valid @RequestBody GenerateArticleRequest request) {
        GenerationPolicy policy = new GenerationPolicy(request.language().trim(), request.tone().trim(),
                request.minCharacters(), request.maxCharacters(), request.maxKeywords(),
                request.requiredKeywords(), request.excludedKeywords(), request.requiredSections());
        var job = jobs.submitArticleGeneration(actors.currentActor(), projectId, policy, idempotencyKey);
        return ResponseEntity.accepted()
                .header(HttpHeaders.LOCATION, "/api/v1/jobs/" + job.id())
                .body(JobResponse.from(job));
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
