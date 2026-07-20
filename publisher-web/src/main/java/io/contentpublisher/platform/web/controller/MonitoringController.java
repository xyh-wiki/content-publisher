package io.contentpublisher.platform.web.controller;

import io.contentpublisher.platform.application.MonitoringApplicationService;
import io.contentpublisher.platform.application.MonitoringSnapshot;
import io.contentpublisher.platform.application.MonitoringWindow;
import io.contentpublisher.platform.web.security.RequestActorProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/monitoring")
public class MonitoringController {
    private final MonitoringApplicationService monitoring;
    private final RequestActorProvider actors;

    public MonitoringController(MonitoringApplicationService monitoring, RequestActorProvider actors) {
        this.monitoring = monitoring;
        this.actors = actors;
    }

    @GetMapping("/summary")
    public MonitoringSnapshot summary(@RequestParam(name = "range", required = false) String range) {
        return monitoring.snapshot(actors.currentActor(), MonitoringWindow.fromCode(range));
    }
}
