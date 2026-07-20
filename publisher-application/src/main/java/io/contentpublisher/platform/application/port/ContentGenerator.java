package io.contentpublisher.platform.application.port;

import io.contentpublisher.platform.domain.GenerationPolicy;
import io.contentpublisher.platform.domain.RepositorySnapshot;

import java.util.List;

public interface ContentGenerator {
    GeneratedContent generate(RepositorySnapshot snapshot, GenerationPolicy policy);

    record GeneratedContent(String title, String summary, String markdown, List<String> keywords) {
        public GeneratedContent {
            keywords = keywords == null ? List.of() : List.copyOf(keywords);
        }
    }
}
