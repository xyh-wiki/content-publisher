package io.contentpublisher.platform.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record Project(
        UUID id,
        String tenantId,
        String gitUrl,
        String name,
        String description,
        String defaultBranch,
        String revision,
        List<String> languages,
        String license,
        ProjectStatus status,
        String createdBy,
        String updatedBy,
        Instant createdAt,
        Instant updatedAt) {
    public Project {
        languages = languages == null ? List.of() : List.copyOf(languages);
    }
}
