package io.contentpublisher.platform.web.controller;

import io.contentpublisher.platform.application.JobApplicationService;
import io.contentpublisher.platform.domain.Job;
import io.contentpublisher.platform.domain.JobStatus;
import io.contentpublisher.platform.web.dto.JobProgressView;
import io.contentpublisher.platform.web.security.RequestActorProvider;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Controller
public class JobPortalController {
    private static final int LIST_LIMIT = 50;
    private static final DateTimeFormatter JOB_TIME_FORMAT = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'").withZone(ZoneOffset.UTC);

    private final JobApplicationService jobs;
    private final RequestActorProvider actors;

    public JobPortalController(JobApplicationService jobs, RequestActorProvider actors) {
        this.jobs = jobs;
        this.actors = actors;
    }

    @GetMapping("/jobs")
    public String jobQueue(Model model) {
        model.addAttribute("jobs", jobs.listJobs(actors.currentActor(), LIST_LIMIT));
        model.addAttribute("jobTypeNames", PortalLabels.jobTypeNames());
        model.addAttribute("jobStatusNames", PortalLabels.jobStatusNames());
        return "jobs";
    }

    @GetMapping("/jobs/{jobId}")
    public String job(@PathVariable UUID jobId, Model model) {
        Job job = jobs.getJob(actors.currentActor(), jobId);
        model.addAttribute("job", job);
        model.addAttribute("active", job.status() == JobStatus.PENDING || job.status() == JobStatus.RUNNING
                || job.status() == JobStatus.RETRY_WAIT);
        model.addAttribute("progress", JobProgressView.from(job));
        model.addAttribute("resultLink", resultLink(job));
        model.addAttribute("jobDisplayName", PortalLabels.jobTypeNames().get(job.type()));
        model.addAttribute("jobStatusLabel", PortalLabels.jobStatusNames().get(job.status()));
        model.addAttribute("jobCreatedAt", JOB_TIME_FORMAT.format(job.createdAt()));
        model.addAttribute("jobUpdatedAt", JOB_TIME_FORMAT.format(job.updatedAt()));
        return "job-detail";
    }

    private String resultLink(Job job) {
        if (job.status() != JobStatus.SUCCEEDED || job.resultResourceId() == null) return null;
        return switch (job.type()) {
            case IMPORT_PROJECT -> "/projects/" + job.resultResourceId();
            case GENERATE_ARTICLE, GENERATE_TOPIC_ARTICLE, GENERATE_WEBSITE_ARTICLE ->
                    "/articles/" + job.resultResourceId();
            case PUBLISH_ARTICLE -> null;
        };
    }
}
