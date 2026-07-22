package io.contentpublisher.platform;

import io.contentpublisher.platform.application.ApplicationException;
import io.contentpublisher.platform.application.JobApplicationService;
import io.contentpublisher.platform.application.port.JobRepository;
import io.contentpublisher.platform.domain.ActorContext;
import io.contentpublisher.platform.domain.Job;
import io.contentpublisher.platform.domain.JobPayload;
import io.contentpublisher.platform.domain.JobStatus;
import io.contentpublisher.platform.domain.JobType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:jobs;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "publisher.security.mode=DISABLED",
        "publisher.jobs.worker-enabled=false",
        "publisher.jobs.max-active-jobs-per-tenant=1",
        "publisher.jobs.max-attempts=3"
})
class DurableJobIntegrationTest {
    @Autowired JobApplicationService service;
    @Autowired JobRepository jobs;

    @Test
    void shouldEnforceIdempotencyAndTenantQuota() {
        ActorContext actor = new ActorContext("tenant-jobs", "editor");
        Job first = service.submitImport(actor, "https://github.com/contentpublisher/platform.git", null,
                "import-platform-001");
        Job repeated = service.submitImport(actor, "https://github.com/contentpublisher/platform.git", null,
                "import-platform-001");

        assertThat(repeated.id()).isEqualTo(first.id());
        assertThatThrownBy(() -> service.submitImport(actor,
                "https://github.com/contentpublisher/another.git", null, "import-platform-001"))
                .isInstanceOfSatisfying(ApplicationException.class,
                        exception -> assertThat(exception.code()).isEqualTo("IDEMPOTENCY_KEY_CONFLICT"));
        assertThatThrownBy(() -> service.submitImport(actor,
                "https://github.com/contentpublisher/second.git", null, "import-platform-002"))
                .isInstanceOfSatisfying(ApplicationException.class,
                        exception -> assertThat(exception.code()).isEqualTo("TENANT_JOB_QUOTA_EXCEEDED"));
    }

    @Test
    void shouldClaimRetryAndCompleteDurableJob() {
        Instant created = Instant.parse("2026-07-20T00:00:00Z");
        Job pending = new Job(UUID.randomUUID(), "tenant-state", "editor", JobType.IMPORT_PROJECT,
                JobStatus.PENDING, new JobPayload.ImportProject("https://github.com/contentpublisher/platform.git", null),
                "state-machine-001", "a".repeat(64), 0, 3, 5, "等待执行", "等待后台工作器领取", null,
                created, null, null, null,
                null, null, created, created);
        jobs.save(pending);

        Job claimed = jobs.claimNext("worker-a", created.plusSeconds(1), created.minusSeconds(300)).orElseThrow();
        assertThat(claimed.status()).isEqualTo(JobStatus.RUNNING);
        assertThat(claimed.attempt()).isEqualTo(1);
        assertThat(claimed.progressPercent()).isEqualTo(12);
        assertThat(jobs.updateProgress(claimed.id(), "worker-a", 55, "生成内容", "正在调用内容生成服务",
                created.plusSeconds(2))).isTrue();
        Job progressing = jobs.findJobById("tenant-state", pending.id()).orElseThrow();
        assertThat(progressing.progressPercent()).isEqualTo(55);
        assertThat(progressing.progressLabel()).isEqualTo("生成内容");

        Instant retryAt = created.plusSeconds(20);
        assertThat(jobs.markRetryWaiting(claimed.id(), "worker-a", retryAt,
                "AI_REQUEST_FAILED", "temporary", created.plusSeconds(3))).isTrue();
        assertThat(jobs.claimNext("worker-b", created.plusSeconds(10), created.minusSeconds(300))).isEmpty();

        Job retried = jobs.claimNext("worker-b", retryAt, created.minusSeconds(300)).orElseThrow();
        UUID resultId = UUID.randomUUID();
        assertThat(retried.attempt()).isEqualTo(2);
        assertThat(jobs.markSucceeded(retried.id(), "worker-b", resultId, retryAt.plusSeconds(1))).isTrue();

        Job completed = jobs.findJobById("tenant-state", pending.id()).orElseThrow();
        assertThat(completed.status()).isEqualTo(JobStatus.SUCCEEDED);
        assertThat(completed.progressPercent()).isEqualTo(100);
        assertThat(completed.progressLabel()).isEqualTo("执行完成");
        assertThat(completed.resultResourceId()).isEqualTo(resultId);
    }

    @Test
    void shouldFailExpiredLeaseAfterAttemptsAreExhausted() {
        Instant created = Instant.parse("2026-07-20T00:00:00Z");
        Job abandoned = new Job(UUID.randomUUID(), "tenant-expired", "editor", JobType.IMPORT_PROJECT,
                JobStatus.RUNNING, new JobPayload.ImportProject("https://github.com/contentpublisher/platform.git", null),
                "expired-lease-001", "b".repeat(64), 3, 3, 88, "正在分析仓库", "读取仓库内容", null,
                created, created, "dead-worker",
                null, null, null, created, created);
        jobs.save(abandoned);

        assertThat(jobs.claimNext("worker-new", created.plusSeconds(600), created.plusSeconds(300))).isEmpty();
        Job failed = jobs.findJobById("tenant-expired", abandoned.id()).orElseThrow();
        assertThat(failed.status()).isEqualTo(JobStatus.FAILED);
        assertThat(failed.errorCode()).isEqualTo("WORKER_LEASE_EXPIRED");
    }

    @Test
    void shouldRejectWholeBatchWhenActiveJobQuotaIsInsufficient() {
        Instant created = Instant.parse("2026-07-20T00:00:00Z");
        String tenantId = "tenant-batch-quota";
        Job first = pendingPublication(tenantId, "batch-child-001", created);
        Job second = pendingPublication(tenantId, "batch-child-002", created);

        assertThat(jobs.createBatchIfWithinQuota(List.of(first, second), 1)).isEmpty();
        assertThat(jobs.findJobById(tenantId, first.id())).isEmpty();
        assertThat(jobs.findJobById(tenantId, second.id())).isEmpty();
    }

    @Test
    void shouldWaitUntilScheduledTimeBeforeClaiming() {
        Instant created = Instant.parse("2026-07-21T00:00:00Z");
        Instant scheduledAt = created.plusSeconds(3_600);
        Job scheduled = pendingPublication("tenant-scheduled", "scheduled-publication-001", created, scheduledAt);
        jobs.save(scheduled);

        assertThat(jobs.claimNext("worker-scheduled", scheduledAt.minusSeconds(1),
                created.minusSeconds(300))).isEmpty();

        Job claimed = jobs.claimNext("worker-scheduled", scheduledAt, created.minusSeconds(300)).orElseThrow();
        assertThat(claimed.id()).isEqualTo(scheduled.id());
        assertThat(claimed.status()).isEqualTo(JobStatus.RUNNING);
        assertThat(claimed.scheduledAt()).isEqualTo(scheduledAt);
    }

    @Test
    void shouldCancelPendingJobButRejectRunningJob() {
        Instant created = Instant.parse("2026-07-21T02:00:00Z");
        String tenantId = "tenant-cancel";
        ActorContext actor = new ActorContext(tenantId, "editor");
        Job pending = pendingPublication(tenantId, "cancel-publication-001", created,
                created.plusSeconds(3_600));
        jobs.save(pending);

        Job cancelled = service.cancelJob(actor, pending.id());
        assertThat(cancelled.status()).isEqualTo(JobStatus.CANCELLED);
        assertThat(cancelled.progressPercent()).isEqualTo(100);
        assertThat(cancelled.progressLabel()).isEqualTo("任务已取消");

        Job running = new Job(UUID.randomUUID(), tenantId, "editor", JobType.PUBLISH_ARTICLE,
                JobStatus.RUNNING, new JobPayload.PublishArticle(UUID.randomUUID(), UUID.randomUUID(), null),
                "cancel-publication-002", "d".repeat(64), 1, 3, 35, "调用渠道接口", "等待平台响应",
                UUID.randomUUID(), created, created, "worker-running", null, null, null, created, created);
        jobs.save(running);

        assertThatThrownBy(() -> service.cancelJob(actor, running.id()))
                .isInstanceOfSatisfying(ApplicationException.class,
                        exception -> assertThat(exception.code()).isEqualTo("JOB_NOT_CANCELLABLE"));
        assertThat(jobs.findJobById(tenantId, running.id()).orElseThrow().status()).isEqualTo(JobStatus.RUNNING);
    }

    private Job pendingPublication(String tenantId, String idempotencyKey, Instant created) {
        return pendingPublication(tenantId, idempotencyKey, created, created);
    }

    private Job pendingPublication(String tenantId, String idempotencyKey, Instant created, Instant scheduledAt) {
        return new Job(UUID.randomUUID(), tenantId, "editor", JobType.PUBLISH_ARTICLE, JobStatus.PENDING,
                new JobPayload.PublishArticle(UUID.randomUUID(), UUID.randomUUID(), null), idempotencyKey,
                "c".repeat(64), 0, 3, 5, "等待执行", "等待后台工作器领取", UUID.randomUUID(),
                scheduledAt, null, null, null, null, null, created, created);
    }
}
