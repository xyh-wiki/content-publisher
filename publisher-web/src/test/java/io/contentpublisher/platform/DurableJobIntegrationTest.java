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
        "publisher.security.enabled=false",
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
                "state-machine-001", "a".repeat(64), 0, 3, created, null, null, null,
                null, null, created, created);
        jobs.save(pending);

        Job claimed = jobs.claimNext("worker-a", created.plusSeconds(1), created.minusSeconds(300)).orElseThrow();
        assertThat(claimed.status()).isEqualTo(JobStatus.RUNNING);
        assertThat(claimed.attempt()).isEqualTo(1);

        Instant retryAt = created.plusSeconds(20);
        assertThat(jobs.markRetryWaiting(claimed.id(), "worker-a", retryAt,
                "AI_REQUEST_FAILED", "temporary", created.plusSeconds(2))).isTrue();
        assertThat(jobs.claimNext("worker-b", created.plusSeconds(10), created.minusSeconds(300))).isEmpty();

        Job retried = jobs.claimNext("worker-b", retryAt, created.minusSeconds(300)).orElseThrow();
        UUID resultId = UUID.randomUUID();
        assertThat(retried.attempt()).isEqualTo(2);
        assertThat(jobs.markSucceeded(retried.id(), "worker-b", resultId, retryAt.plusSeconds(1))).isTrue();

        Job completed = jobs.findJobById("tenant-state", pending.id()).orElseThrow();
        assertThat(completed.status()).isEqualTo(JobStatus.SUCCEEDED);
        assertThat(completed.resultResourceId()).isEqualTo(resultId);
    }

    @Test
    void shouldFailExpiredLeaseAfterAttemptsAreExhausted() {
        Instant created = Instant.parse("2026-07-20T00:00:00Z");
        Job abandoned = new Job(UUID.randomUUID(), "tenant-expired", "editor", JobType.IMPORT_PROJECT,
                JobStatus.RUNNING, new JobPayload.ImportProject("https://github.com/contentpublisher/platform.git", null),
                "expired-lease-001", "b".repeat(64), 3, 3, created, created, "dead-worker",
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

    private Job pendingPublication(String tenantId, String idempotencyKey, Instant created) {
        return new Job(UUID.randomUUID(), tenantId, "editor", JobType.PUBLISH_ARTICLE, JobStatus.PENDING,
                new JobPayload.PublishArticle(UUID.randomUUID(), UUID.randomUUID(), null), idempotencyKey,
                "c".repeat(64), 0, 3, created, null, null, null, null, null, created, created);
    }
}
