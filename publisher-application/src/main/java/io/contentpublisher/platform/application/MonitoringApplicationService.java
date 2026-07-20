package io.contentpublisher.platform.application;

import io.contentpublisher.platform.application.port.MonitoringQuery;
import io.contentpublisher.platform.domain.ActorContext;

import java.time.Clock;
import java.time.Instant;

public final class MonitoringApplicationService {
    private final MonitoringQuery monitoring;
    private final Clock clock;

    public MonitoringApplicationService(MonitoringQuery monitoring, Clock clock) {
        this.monitoring = monitoring;
        this.clock = clock;
    }

    public MonitoringSnapshot snapshot(ActorContext actor, MonitoringWindow window) {
        Instant capturedAt = clock.instant();
        return monitoring.load(actor.tenantId(), window, capturedAt.minus(window.duration()), capturedAt);
    }
}
