package io.contentpublisher.platform.application.port;

import io.contentpublisher.platform.domain.GenerationPolicy;
import io.contentpublisher.platform.domain.RepositorySnapshot;
import io.contentpublisher.platform.domain.TopicBrief;
import io.contentpublisher.platform.domain.WebsiteBrief;
import io.contentpublisher.platform.domain.WebsiteSnapshot;

import java.util.List;

public interface ContentGenerator {
    GeneratedContent generate(String tenantId, RepositorySnapshot snapshot, GenerationPolicy policy);
    GeneratedContent generateFromBrief(String tenantId, TopicBrief brief, GenerationPolicy policy);
    GeneratedContent generateFromWebsite(String tenantId, WebsiteBrief brief, WebsiteSnapshot snapshot,
                                         GenerationPolicy policy);

    record GeneratedContent(String title, String summary, String markdown, List<String> keywords) {
        public GeneratedContent {
            keywords = keywords == null ? List.of() : List.copyOf(keywords);
        }
    }
}
