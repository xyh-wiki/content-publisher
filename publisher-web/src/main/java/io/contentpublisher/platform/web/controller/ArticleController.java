package io.contentpublisher.platform.web.controller;

import io.contentpublisher.platform.application.JobApplicationService;
import io.contentpublisher.platform.application.PublishingApplicationService;
import io.contentpublisher.platform.web.dto.ArticleResponse;
import io.contentpublisher.platform.web.dto.JobResponse;
import io.contentpublisher.platform.web.dto.PublishArticleRequest;
import io.contentpublisher.platform.web.dto.RejectArticleRequest;
import io.contentpublisher.platform.web.dto.UpdateArticleRequest;
import io.contentpublisher.platform.web.dto.ArticleVersionResponse;
import io.contentpublisher.platform.web.security.RequestActorProvider;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.List;

@RestController
@RequestMapping("/api/v1/articles")
public class ArticleController {
    private final PublishingApplicationService publishing;
    private final JobApplicationService jobs;
    private final RequestActorProvider actors;

    public ArticleController(PublishingApplicationService publishing, JobApplicationService jobs,
                             RequestActorProvider actors) {
        this.publishing = publishing;
        this.jobs = jobs;
        this.actors = actors;
    }

    @GetMapping("/{articleId}")
    public ArticleResponse get(@PathVariable UUID articleId) {
        return ArticleResponse.from(publishing.getArticle(actors.currentActor(), articleId));
    }

    @GetMapping("/{articleId}/versions")
    public List<ArticleVersionResponse> versions(@PathVariable UUID articleId) {
        return publishing.getArticleVersions(actors.currentActor(), articleId).stream()
                .map(ArticleVersionResponse::from).toList();
    }

    @PutMapping("/{articleId}")
    public ArticleResponse update(@PathVariable UUID articleId, @Valid @RequestBody UpdateArticleRequest request) {
        return ArticleResponse.from(publishing.updateArticle(actors.currentActor(), articleId,
                request.expectedVersion(), request.title(), request.summary(), request.markdown(), request.keywords()));
    }

    @PostMapping("/{articleId}/approve")
    public ArticleResponse approve(@PathVariable UUID articleId) {
        return ArticleResponse.from(publishing.approveArticle(actors.currentActor(), articleId));
    }

    @PostMapping("/{articleId}/reject")
    public ArticleResponse reject(@PathVariable UUID articleId, @Valid @RequestBody RejectArticleRequest request) {
        return ArticleResponse.from(publishing.rejectArticle(actors.currentActor(), articleId, request.reason()));
    }

    @PostMapping("/{articleId}/publications")
    public ResponseEntity<JobResponse> publish(@PathVariable UUID articleId,
                                               @RequestHeader("Idempotency-Key") String idempotencyKey,
                                               @Valid @RequestBody PublishArticleRequest request) {
        var job = jobs.submitPublication(actors.currentActor(), articleId, request.channelAccountId(),
                request.canonicalUrl(), idempotencyKey);
        return ResponseEntity.accepted().header(HttpHeaders.LOCATION, "/api/v1/jobs/" + job.id())
                .body(JobResponse.from(job));
    }
}
