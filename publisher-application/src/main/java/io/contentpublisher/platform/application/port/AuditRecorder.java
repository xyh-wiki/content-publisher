package io.contentpublisher.platform.application.port;

import io.contentpublisher.platform.domain.ActorContext;

import java.util.Map;
import java.util.UUID;

public interface AuditRecorder {
    void record(ActorContext actor, String action, String targetType, UUID targetId, Map<String, String> details);
}
