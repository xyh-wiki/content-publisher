package io.contentpublisher.platform.domain;

import java.util.UUID;

public sealed interface JobPayload permits JobPayload.ImportProject, JobPayload.GenerateArticle {
    record ImportProject(String gitUrl, String branch) implements JobPayload {}

    record GenerateArticle(UUID projectId, GenerationPolicy policy) implements JobPayload {}
}
