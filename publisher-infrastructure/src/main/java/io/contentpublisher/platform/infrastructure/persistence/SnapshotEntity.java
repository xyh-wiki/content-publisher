package io.contentpublisher.platform.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "repository_snapshots")
class SnapshotEntity {
    @Id @Column(name = "project_id") UUID projectId;
    @Column(name = "tenant_id", nullable = false, length = 100) String tenantId;
    @Column(nullable = false, length = 255) String name;
    @Column(length = 2000) String description;
    @Column(name = "default_branch", length = 255) String defaultBranch;
    @Column(nullable = false, length = 64) String revision;
    @Column(nullable = false, columnDefinition = "text") String readme;
    @Column(name = "manifest_summary", nullable = false, columnDefinition = "text") String manifestSummary;
    @Column(name = "file_tree_json", nullable = false, columnDefinition = "text") String fileTreeJson;
    @Column(name = "languages_json", nullable = false, columnDefinition = "text") String languagesJson;
    @Column(length = 100) String license;

    protected SnapshotEntity() {}
}
