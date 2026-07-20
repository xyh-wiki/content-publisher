package io.contentpublisher.platform.domain;

import java.util.UUID;

public sealed interface JobPayload permits JobPayload.ImportProject, JobPayload.GenerateArticle, JobPayload.PublishArticle {
    record ImportProject(String gitUrl, String branch) implements JobPayload {}

    record GenerateArticle(UUID projectId, GenerationPolicy policy) implements JobPayload {}

    record PublishArticle(UUID articleId, UUID channelAccountId, String canonicalUrl) implements JobPayload {}
}
