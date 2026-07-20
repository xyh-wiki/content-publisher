package io.contentpublisher.platform.application.port;

import io.contentpublisher.platform.domain.RepositorySnapshot;

public interface RepositoryInspector {
    RepositorySnapshot inspect(String gitUrl, String branch);
}
