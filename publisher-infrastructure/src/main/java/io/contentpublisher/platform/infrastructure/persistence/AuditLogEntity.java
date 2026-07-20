package io.contentpublisher.platform.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
class AuditLogEntity {
    @Id UUID id;
    @Column(name = "tenant_id", nullable = false, length = 100) String tenantId;
    @Column(nullable = false, length = 200) String subject;
    @Column(nullable = false, length = 100) String action;
    @Column(name = "target_type", nullable = false, length = 100) String targetType;
    @Column(name = "target_id", nullable = false) UUID targetId;
    @Column(name = "details_json", nullable = false, columnDefinition = "text") String detailsJson;
    @Column(name = "occurred_at", nullable = false) Instant occurredAt;

    protected AuditLogEntity() {}
}
