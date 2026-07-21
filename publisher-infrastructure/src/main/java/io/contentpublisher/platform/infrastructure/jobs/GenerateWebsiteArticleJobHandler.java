package io.contentpublisher.platform.infrastructure.jobs;

import io.contentpublisher.platform.application.ContentGenerationApplicationService;
import io.contentpublisher.platform.application.JobProgressReporter;
import io.contentpublisher.platform.domain.ActorContext;
import io.contentpublisher.platform.domain.Job;
import io.contentpublisher.platform.domain.JobPayload;
import io.contentpublisher.platform.domain.JobType;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public final class GenerateWebsiteArticleJobHandler implements JobHandler {
    private final ContentGenerationApplicationService generation;

    public GenerateWebsiteArticleJobHandler(ContentGenerationApplicationService generation) {
        this.generation = generation;
    }

    @Override
    public JobType type() {
        return JobType.GENERATE_WEBSITE_ARTICLE;
    }

    @Override
    public UUID handle(Job job, ActorContext actor, JobProgressReporter progress) {
        JobPayload.GenerateWebsiteArticle payload = (JobPayload.GenerateWebsiteArticle) job.payload();
        return generation.generateWebsiteArticle(actor, payload.brief(), payload.policy(), job.id(), progress).id();
    }
}
