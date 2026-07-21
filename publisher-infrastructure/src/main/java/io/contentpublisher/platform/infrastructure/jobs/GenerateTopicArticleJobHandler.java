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
public final class GenerateTopicArticleJobHandler implements JobHandler {
    private final ContentGenerationApplicationService generation;

    public GenerateTopicArticleJobHandler(ContentGenerationApplicationService generation) {
        this.generation = generation;
    }

    @Override
    public JobType type() {
        return JobType.GENERATE_TOPIC_ARTICLE;
    }

    @Override
    public UUID handle(Job job, ActorContext actor, JobProgressReporter progress) {
        JobPayload.GenerateTopicArticle payload = (JobPayload.GenerateTopicArticle) job.payload();
        return generation.generateTopicArticle(actor, payload.brief(), payload.policy(), job.id(), progress).id();
    }
}
