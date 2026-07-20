package io.contentpublisher.platform.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("publisher.jobs")
public record JobProperties(boolean workerEnabled, int maxActiveJobsPerTenant, int maxAttempts,
                            Duration pollInterval, Duration lockTimeout,
                            Duration initialRetryDelay, Duration maxRetryDelay) {
    public JobProperties {
        maxActiveJobsPerTenant = maxActiveJobsPerTenant <= 0 ? 20 : maxActiveJobsPerTenant;
        maxAttempts = maxAttempts <= 0 ? 4 : maxAttempts;
        pollInterval = pollInterval == null ? Duration.ofSeconds(1) : pollInterval;
        lockTimeout = lockTimeout == null ? Duration.ofMinutes(5) : lockTimeout;
        initialRetryDelay = initialRetryDelay == null ? Duration.ofSeconds(10) : initialRetryDelay;
        maxRetryDelay = maxRetryDelay == null ? Duration.ofMinutes(5) : maxRetryDelay;
        if (maxAttempts > 20) throw new IllegalArgumentException("publisher.jobs.max-attempts 不能超过 20");
        if (lockTimeout.compareTo(Duration.ofSeconds(30)) < 0) {
            throw new IllegalArgumentException("publisher.jobs.lock-timeout 不能小于 30 秒");
        }
    }
}
