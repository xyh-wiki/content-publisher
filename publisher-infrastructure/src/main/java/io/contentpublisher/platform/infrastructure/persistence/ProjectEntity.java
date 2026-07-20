package io.contentpublisher.platform.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "projects")
class ProjectEntity {
    @Id UUID id;
    @Column(name = "tenant_id", nullable = false, length = 100) String tenantId;
    @Column(name = "git_url", nullable = false, unique = true, length = 2048) String gitUrl;
    @Column(nullable = false, length = 255) String name;
    @Column(length = 2000) String description;
    @Column(name = "default_branch", length = 255) String defaultBranch;
    @Column(length = 64) String revision;
    @Column(name = "languages_json", nullable = false, columnDefinition = "text") String languagesJson;
    @Column(length = 100) String license;
    @Column(nullable = false, length = 30) String status;
    @Column(name = "created_by", nullable = false, length = 200) String createdBy;
    @Column(name = "updated_by", nullable = false, length = 200) String updatedBy;
    @Column(name = "created_at", nullable = false) Instant createdAt;
    @Column(name = "updated_at", nullable = false) Instant updatedAt;

    protected ProjectEntity() {}
}
