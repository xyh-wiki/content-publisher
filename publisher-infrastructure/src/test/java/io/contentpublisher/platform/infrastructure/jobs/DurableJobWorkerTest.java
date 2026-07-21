package io.contentpublisher.platform.infrastructure.jobs;

import io.contentpublisher.platform.application.ApplicationException;
import io.contentpublisher.platform.application.ProjectImportApplicationService;
import io.contentpublisher.platform.application.ProjectApplicationService;
import io.contentpublisher.platform.application.port.AuditRecorder;
import io.contentpublisher.platform.application.port.JobRepository;
import io.contentpublisher.platform.domain.Job;
import io.contentpublisher.platform.domain.JobPayload;
import io.contentpublisher.platform.domain.JobStatus;
import io.contentpublisher.platform.domain.JobType;
import io.contentpublisher.platform.infrastructure.config.JobProperties;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DurableJobWorkerTest {
    private static final Instant NOW = Instant.parse("2026-07-20T00:00:00Z");

    @Test
    void shouldScheduleRetryForTransientFailure() {
        JobRepository jobs = mock(JobRepository.class);
        ProjectApplicationService projects = mock(ProjectApplicationService.class);
        AuditRecorder audits = mock(AuditRecorder.class);
        Job job = runningJob(1, 3);
        when(jobs.claimNext(anyString(), any(), any())).thenReturn(Optional.of(job));
        when(projects.importProject(any(), anyString(), any(), any())).thenAnswer(invocation -> {
            io.contentpublisher.platform.application.JobProgressReporter progress = invocation.getArgument(3);
            progress.update(35, "读取仓库", "正在读取公开仓库内容");
            throw new ApplicationException("GIT_IMPORT_FAILED", "temporary");
        });
        when(jobs.updateProgress(eq(job.id()), anyString(), eq(35), eq("读取仓库"),
                eq("正在读取公开仓库内容"), eq(NOW))).thenReturn(true);
        when(jobs.markRetryWaiting(eq(job.id()), anyString(), any(), eq("GIT_IMPORT_FAILED"),
                eq("temporary"), eq(NOW))).thenReturn(true);

        worker(jobs, projects, audits).poll();

        verify(jobs).markRetryWaiting(eq(job.id()), anyString(), eq(NOW.plusSeconds(10)),
                eq("GIT_IMPORT_FAILED"), eq("temporary"), eq(NOW));
        verify(jobs).updateProgress(eq(job.id()), anyString(), eq(35), eq("读取仓库"),
                eq("正在读取公开仓库内容"), eq(NOW));
        verify(jobs, never()).markFailed(any(), anyString(), anyString(), anyString(), any());
    }

    @Test
    void shouldFailPermanentlyAfterLastAttempt() {
        JobRepository jobs = mock(JobRepository.class);
        ProjectApplicationService projects = mock(ProjectApplicationService.class);
        AuditRecorder audits = mock(AuditRecorder.class);
        Job job = runningJob(3, 3);
        when(jobs.claimNext(anyString(), any(), any())).thenReturn(Optional.of(job));
        when(projects.importProject(any(), anyString(), any(), any()))
                .thenThrow(new ApplicationException("GIT_IMPORT_FAILED", "temporary"));
        when(jobs.markFailed(eq(job.id()), anyString(), eq("GIT_IMPORT_FAILED"), eq("temporary"), eq(NOW)))
                .thenReturn(true);

        worker(jobs, projects, audits).poll();

        verify(jobs).markFailed(eq(job.id()), anyString(), eq("GIT_IMPORT_FAILED"), eq("temporary"), eq(NOW));
        verify(jobs, never()).markRetryWaiting(any(), anyString(), any(), anyString(), anyString(), any());
    }

    private DurableJobWorker worker(JobRepository jobs, ProjectApplicationService projects, AuditRecorder audits) {
        JobProperties properties = new JobProperties(true, 20, 3, Duration.ofSeconds(1),
                Duration.ofMinutes(5), Duration.ofSeconds(10), Duration.ofMinutes(5));
        ProjectImportApplicationService imports = mock(ProjectImportApplicationService.class);
        when(imports.importProject(any(), anyString(), any(), any())).thenAnswer(invocation ->
                projects.importProject(invocation.getArgument(0), invocation.getArgument(1),
                        invocation.getArgument(2), invocation.getArgument(3)));
        return new DurableJobWorker(jobs, java.util.List.of(new ImportProjectJobHandler(imports)), audits,
                properties, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private Job runningJob(int attempt, int maxAttempts) {
        return new Job(UUID.randomUUID(), "tenant-worker", "editor", JobType.IMPORT_PROJECT, JobStatus.RUNNING,
                new JobPayload.ImportProject("https://github.com/contentpublisher/platform.git", null),
                "worker-test-001", "a".repeat(64), attempt, maxAttempts, 12, "任务已领取", "准备执行", null,
                NOW, NOW, "worker-old",
                null, null, null, NOW, NOW);
    }
}
