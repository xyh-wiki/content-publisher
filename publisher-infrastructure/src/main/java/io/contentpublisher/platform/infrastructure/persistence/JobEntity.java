package io.contentpublisher.platform.infrastructure.persistence;

import io.contentpublisher.platform.domain.JobStatus;
import io.contentpublisher.platform.domain.JobType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "jobs")
class JobEntity {
    @Id UUID id;
    @Column(name = "tenant_id", nullable = false, length = 100) String tenantId;
    @Column(name = "actor_subject", nullable = false, length = 200) String actorSubject;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40) JobType type;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) JobStatus status;
    @Column(name = "payload_json", nullable = false, columnDefinition = "text") String payloadJson;
    @Column(name = "idempotency_key", nullable = false, length = 128) String idempotencyKey;
    @Column(name = "request_hash", nullable = false, length = 64) String requestHash;
    @Column(nullable = false) int attempt;
    @Column(name = "max_attempts", nullable = false) int maxAttempts;
    @Column(name = "scheduled_at", nullable = false) Instant scheduledAt;
    @Column(name = "locked_at") Instant lockedAt;
    @Column(name = "lock_owner", length = 200) String lockOwner;
    @Column(name = "result_resource_id") UUID resultResourceId;
    @Column(name = "error_code", length = 100) String errorCode;
    @Column(name = "error_message", length = 2000) String errorMessage;
    @Column(name = "created_at", nullable = false) Instant createdAt;
    @Column(name = "updated_at", nullable = false) Instant updatedAt;
    @Version @Column(nullable = false) long version;

    protected JobEntity() {}
}
