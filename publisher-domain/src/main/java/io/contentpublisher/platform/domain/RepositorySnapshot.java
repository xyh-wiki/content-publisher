package io.contentpublisher.platform.domain;

import java.util.List;

public record RepositorySnapshot(
        String name,
        String description,
        String defaultBranch,
        String revision,
        String readme,
        String manifestSummary,
        List<String> fileTree,
        List<String> languages,
        String license) {
    public RepositorySnapshot {
        fileTree = fileTree == null ? List.of() : List.copyOf(fileTree);
        languages = languages == null ? List.of() : List.copyOf(languages);
    }
}
