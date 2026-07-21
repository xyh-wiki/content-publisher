package io.contentpublisher.platform.infrastructure.jobs;

import io.contentpublisher.platform.application.JobProgressReporter;
import io.contentpublisher.platform.domain.ActorContext;
import io.contentpublisher.platform.domain.Job;
import io.contentpublisher.platform.domain.JobType;

import java.util.UUID;

public interface JobHandler {
    JobType type();

    UUID handle(Job job, ActorContext actor, JobProgressReporter progress);
}
