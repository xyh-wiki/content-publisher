package io.contentpublisher.platform.application.port;

import io.contentpublisher.platform.application.MonitoringSnapshot;
import io.contentpublisher.platform.application.MonitoringWindow;

import java.time.Instant;

public interface MonitoringQuery {
    MonitoringSnapshot load(String tenantId, MonitoringWindow window, Instant windowStart, Instant capturedAt);
}
