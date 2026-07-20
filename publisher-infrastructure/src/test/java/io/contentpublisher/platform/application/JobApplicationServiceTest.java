package io.contentpublisher.platform.application;

import io.contentpublisher.platform.application.port.AuditRecorder;
import io.contentpublisher.platform.application.port.JobRepository;
import io.contentpublisher.platform.domain.ActorContext;
import io.contentpublisher.platform.domain.Job;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JobApplicationServiceTest {
    @Test
    void shouldIncludeCanonicalUrlInPublicationIdempotencyHash() {
        JobRepository jobs = mock(JobRepository.class);
        ProjectApplicationService projects = mock(ProjectApplicationService.class);
        PublishingApplicationService publishing = mock(PublishingApplicationService.class);
        AuditRecorder audits = mock(AuditRecorder.class);
        AtomicReference<Job> stored = new AtomicReference<>();
        when(publishing.validateCanonicalUrl(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        when(jobs.findByIdempotencyKey("tenant", "publication-key-001"))
                .thenAnswer(invocation -> Optional.ofNullable(stored.get()));
        when(jobs.createIfWithinQuota(any(Job.class), anyInt())).thenAnswer(invocation -> {
            Job job = invocation.getArgument(0);
            stored.set(job);
            return Optional.of(job);
        });
        JobApplicationService service = new JobApplicationService(jobs, projects, publishing, audits,
                Clock.fixed(Instant.parse("2026-07-20T00:00:00Z"), ZoneOffset.UTC), 20, 4);
        ActorContext actor = new ActorContext("tenant", "editor");
        UUID articleId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();

        Job first = service.submitPublication(actor, articleId, accountId,
                "https://example.com/first", "publication-key-001");

        assertThat(first).isSameAs(stored.get());
        assertThatThrownBy(() -> service.submitPublication(actor, articleId, accountId,
                "https://example.com/second", "publication-key-001"))
                .isInstanceOfSatisfying(ApplicationException.class,
                        exception -> assertThat(exception.code()).isEqualTo("IDEMPOTENCY_KEY_CONFLICT"));
    }

    @Test
    void shouldSubmitMultiplePublicationJobsIdempotently() {
        JobRepository jobs = mock(JobRepository.class);
        ProjectApplicationService projects = mock(ProjectApplicationService.class);
        PublishingApplicationService publishing = mock(PublishingApplicationService.class);
        AuditRecorder audits = mock(AuditRecorder.class);
        Map<String, Job> stored = new LinkedHashMap<>();
        when(publishing.validateCanonicalUrl("https://example.com/original"))
                .thenReturn("https://example.com/original");
        when(jobs.findByIdempotencyKey(anyString(), anyString())).thenAnswer(invocation ->
                Optional.ofNullable(stored.get(invocation.getArgument(1))));
        when(jobs.createBatchIfWithinQuota(any(), anyInt())).thenAnswer(invocation -> {
            List<Job> batch = invocation.getArgument(0);
            batch.forEach(job -> stored.put(job.idempotencyKey(), job));
            return Optional.of(batch);
        });
        JobApplicationService service = new JobApplicationService(jobs, projects, publishing, audits,
                Clock.fixed(Instant.parse("2026-07-20T00:00:00Z"), ZoneOffset.UTC), 20, 4);
        ActorContext actor = new ActorContext("tenant", "editor");
        UUID articleId = UUID.randomUUID();
        List<UUID> accountIds = List.of(UUID.randomUUID(), UUID.randomUUID());

        List<Job> first = service.submitPublications(actor, articleId, accountIds,
                "https://example.com/original", "publication-batch-001");
        List<Job> repeated = service.submitPublications(actor, articleId, accountIds,
                "https://example.com/original", "publication-batch-001");

        assertThat(first).hasSize(2).extracting(Job::idempotencyKey)
                .allMatch(key -> key.startsWith("publication-batch:"));
        assertThat(repeated).extracting(Job::id).containsExactlyElementsOf(first.stream().map(Job::id).toList());
        assertThat(stored).hasSize(2);
    }
}
