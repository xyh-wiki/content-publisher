package io.contentpublisher.platform.infrastructure.jobs;

import io.contentpublisher.platform.application.ApplicationException;
import io.contentpublisher.platform.application.JobProgressReporter;
import io.contentpublisher.platform.application.port.AuditRecorder;
import io.contentpublisher.platform.application.port.JobRepository;
import io.contentpublisher.platform.domain.ActorContext;
import io.contentpublisher.platform.domain.Job;
import io.contentpublisher.platform.domain.JobType;
import io.contentpublisher.platform.infrastructure.config.JobProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "publisher.jobs.worker-enabled", havingValue = "true", matchIfMissing = true)
public class DurableJobWorker {
    private static final Logger log = LoggerFactory.getLogger(DurableJobWorker.class);
    private static final Set<String> RETRYABLE_CODES = Set.of(
            "AI_REQUEST_FAILED", "AI_REQUEST_INTERRUPTED", "GIT_IMPORT_FAILED",
            "WEBSITE_FETCH_FAILED", "WEBSITE_FETCH_INTERRUPTED", "AI_META_NARRATION_REJECTED");

    private final JobRepository jobs;
    private final Map<JobType, JobHandler> handlers;
    private final AuditRecorder auditRecorder;
    private final JobProperties properties;
    private final Clock clock;
    private final String workerId;

    public DurableJobWorker(JobRepository jobs, List<JobHandler> handlers, AuditRecorder auditRecorder,
                            JobProperties properties, Clock clock) {
        this.jobs = jobs;
        this.handlers = handlerRegistry(handlers);
        this.auditRecorder = auditRecorder;
        this.properties = properties;
        this.clock = clock;
        this.workerId = resolveWorkerId();
    }

    @Scheduled(fixedDelayString = "${publisher.jobs.poll-interval:1s}")
    public void poll() {
        Instant now = clock.instant();
        jobs.claimNext(workerId, now, now.minus(properties.lockTimeout())).ifPresent(this::execute);
    }

    private void execute(Job job) {
        ActorContext actor = new ActorContext(job.tenantId(), job.actorSubject());
        JobProgressReporter progress = progressReporter(job);
        try {
            JobHandler handler = handlers.get(job.type());
            if (handler == null) {
                throw new ApplicationException("JOB_HANDLER_NOT_FOUND", "任务类型尚未注册执行器: " + job.type());
            }
            UUID resultId = handler.handle(job, actor, progress);
            Instant completedAt = clock.instant();
            if (jobs.markSucceeded(job.id(), workerId, resultId, completedAt)) {
                auditRecorder.record(actor, "JOB_SUCCEEDED", "JOB", job.id(),
                        Map.of("jobType", job.type().name(), "resultResourceId", resultId.toString(),
                                "attempt", Integer.toString(job.attempt())));
            }
        } catch (ApplicationException exception) {
            handleFailure(job, actor, exception.code(), exception.getMessage(), RETRYABLE_CODES.contains(exception.code()));
        } catch (RuntimeException exception) {
            log.error("Unexpected job execution failure jobId={} type={}", job.id(), job.type(), exception);
            handleFailure(job, actor, "INTERNAL_JOB_ERROR", "任务执行发生内部错误", false);
        }
    }

    private JobProgressReporter progressReporter(Job job) {
        return (percent, label, detail) -> {
            boolean updated = jobs.updateProgress(job.id(), workerId, percent, label, detail, clock.instant());
            if (!updated) {
                log.warn("Job progress update skipped because lease is no longer owned jobId={} workerId={}",
                        job.id(), workerId);
            }
        };
    }

    private void handleFailure(Job job, ActorContext actor, String code, String message, boolean retryable) {
        Instant now = clock.instant();
        if (retryable && job.attempt() < job.maxAttempts()) {
            Duration delay = retryDelay(job.attempt());
            if (jobs.markRetryWaiting(job.id(), workerId, now.plus(delay), code, message, now)) {
                auditRecorder.record(actor, "JOB_RETRY_SCHEDULED", "JOB", job.id(),
                        Map.of("errorCode", code, "attempt", Integer.toString(job.attempt()),
                                "retryDelaySeconds", Long.toString(delay.toSeconds())));
            }
            return;
        }
        if (jobs.markFailed(job.id(), workerId, code, message, now)) {
            auditRecorder.record(actor, "JOB_FAILED", "JOB", job.id(),
                    Map.of("errorCode", code, "attempt", Integer.toString(job.attempt())));
        }
    }

    private Duration retryDelay(int attempt) {
        long multiplier = 1L << Math.min(Math.max(attempt - 1, 0), 20);
        Duration calculated;
        try {
            calculated = properties.initialRetryDelay().multipliedBy(multiplier);
        } catch (ArithmeticException exception) {
            calculated = properties.maxRetryDelay();
        }
        return calculated.compareTo(properties.maxRetryDelay()) > 0 ? properties.maxRetryDelay() : calculated;
    }

    private Map<JobType, JobHandler> handlerRegistry(List<JobHandler> candidates) {
        Map<JobType, JobHandler> registry = new EnumMap<>(JobType.class);
        for (JobHandler handler : candidates) {
            JobHandler previous = registry.putIfAbsent(handler.type(), handler);
            if (previous != null) {
                throw new IllegalStateException("任务类型存在重复执行器: " + handler.type());
            }
        }
        return Map.copyOf(registry);
    }

    private String resolveWorkerId() {
        try {
            return InetAddress.getLocalHost().getHostName() + "-" + UUID.randomUUID();
        } catch (Exception ignored) {
            return "worker-" + UUID.randomUUID();
        }
    }
}
