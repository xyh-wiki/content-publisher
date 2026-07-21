package io.contentpublisher.platform.infrastructure.jobs;

import io.contentpublisher.platform.application.JobProgressReporter;
import io.contentpublisher.platform.application.ProjectImportApplicationService;
import io.contentpublisher.platform.domain.ActorContext;
import io.contentpublisher.platform.domain.Job;
import io.contentpublisher.platform.domain.JobPayload;
import io.contentpublisher.platform.domain.JobType;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public final class ImportProjectJobHandler implements JobHandler {
    private final ProjectImportApplicationService projects;

    public ImportProjectJobHandler(ProjectImportApplicationService projects) {
        this.projects = projects;
    }

    @Override
    public JobType type() {
        return JobType.IMPORT_PROJECT;
    }

    @Override
    public UUID handle(Job job, ActorContext actor, JobProgressReporter progress) {
        JobPayload.ImportProject payload = (JobPayload.ImportProject) job.payload();
        return projects.importProject(actor, payload.gitUrl(), payload.branch(), progress).id();
    }
}
