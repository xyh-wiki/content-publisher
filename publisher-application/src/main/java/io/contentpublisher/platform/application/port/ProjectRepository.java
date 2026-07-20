package io.contentpublisher.platform.application.port;

import io.contentpublisher.platform.domain.Project;

import java.util.Optional;
import java.util.UUID;

public interface ProjectRepository {
    Project save(Project project);
    Optional<Project> findProjectById(String tenantId, UUID id);
    Optional<Project> findByGitUrl(String tenantId, String gitUrl);
}
