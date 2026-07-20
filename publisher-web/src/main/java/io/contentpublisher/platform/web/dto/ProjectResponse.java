package io.contentpublisher.platform.web.dto;

import io.contentpublisher.platform.domain.Project;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ProjectResponse(UUID id, String gitUrl, String name, String description, String defaultBranch,
                              String revision, List<String> languages, String license, String status,
                              Instant createdAt, Instant updatedAt) {
    public static ProjectResponse from(Project project) {
        return new ProjectResponse(project.id(), project.gitUrl(), project.name(), project.description(),
                project.defaultBranch(), project.revision(), project.languages(), project.license(),
                project.status().name(), project.createdAt(), project.updatedAt());
    }
}
