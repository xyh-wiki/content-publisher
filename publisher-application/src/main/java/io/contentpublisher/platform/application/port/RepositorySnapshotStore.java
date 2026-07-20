package io.contentpublisher.platform.application.port;

import io.contentpublisher.platform.domain.RepositorySnapshot;

import java.util.Optional;
import java.util.UUID;

public interface RepositorySnapshotStore {
    void save(String tenantId, UUID projectId, RepositorySnapshot snapshot);
    Optional<RepositorySnapshot> findByProjectId(String tenantId, UUID projectId);
}
