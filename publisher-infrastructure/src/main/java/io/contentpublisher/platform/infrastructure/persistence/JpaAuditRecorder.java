package io.contentpublisher.platform.infrastructure.persistence;

import io.contentpublisher.platform.application.port.AuditRecorder;
import io.contentpublisher.platform.domain.ActorContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Map;
import java.util.UUID;

@Repository
@Transactional
public class JpaAuditRecorder implements AuditRecorder {
    private final AuditLogJpaRepository auditLogs;
    private final JpaDomainMapper mapper;
    private final Clock clock;

    public JpaAuditRecorder(AuditLogJpaRepository auditLogs, JpaDomainMapper mapper, Clock clock) {
        this.auditLogs = auditLogs;
        this.mapper = mapper;
        this.clock = clock;
    }

    @Override
    public void record(ActorContext actor, String action, String targetType, UUID targetId,
                       Map<String, String> details) {
        AuditLogEntity entity = new AuditLogEntity();
        entity.id = UUID.randomUUID(); entity.tenantId = actor.tenantId(); entity.subject = actor.subject();
        entity.action = action; entity.targetType = targetType; entity.targetId = targetId;
        entity.detailsJson = mapper.mapJson(details); entity.occurredAt = clock.instant();
        auditLogs.save(entity);
    }
}
