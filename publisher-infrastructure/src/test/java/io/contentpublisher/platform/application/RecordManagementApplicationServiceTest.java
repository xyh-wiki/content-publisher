package io.contentpublisher.platform.application;

import io.contentpublisher.platform.application.port.ArticleRepository;
import io.contentpublisher.platform.application.port.AuditRecorder;
import io.contentpublisher.platform.application.port.JobRepository;
import io.contentpublisher.platform.domain.ActorContext;
import io.contentpublisher.platform.domain.Article;
import io.contentpublisher.platform.domain.ArticleStatus;
import io.contentpublisher.platform.domain.Job;
import io.contentpublisher.platform.domain.JobPayload;
import io.contentpublisher.platform.domain.JobStatus;
import io.contentpublisher.platform.domain.JobType;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RecordManagementApplicationServiceTest {
    private static final ActorContext ACTOR = new ActorContext("tenant", "admin");
    private static final Instant NOW = Instant.parse("2026-07-20T00:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Test
    void shouldDeleteArticleGraphBeforeLinkedTerminalJobs() {
        ArticleRepository articles = mock(ArticleRepository.class);
        JobRepository jobs = mock(JobRepository.class);
        AuditRecorder audits = mock(AuditRecorder.class);
        Article article = article();
        Job generation = job(article.generationJobId(), JobStatus.SUCCEEDED, JobType.GENERATE_ARTICLE,
                new JobPayload.GenerateArticle(article.projectId(), null), article.id());
        Job publication = job(UUID.randomUUID(), JobStatus.FAILED, JobType.PUBLISH_ARTICLE,
                new JobPayload.PublishArticle(article.id(), UUID.randomUUID(), null), null);
        when(articles.findArticleById("tenant", article.id())).thenReturn(Optional.of(article));
        when(jobs.findByArticleReference("tenant", article.id())).thenReturn(List.of(generation, publication));
        when(articles.softDeleteArticleRecord("tenant", article.id(), "admin", NOW)).thenReturn(true);
        when(jobs.softDeleteJobRecord("tenant", generation.id(), "admin", NOW)).thenReturn(true);
        when(jobs.softDeleteJobRecord("tenant", publication.id(), "admin", NOW)).thenReturn(true);

        service(articles, jobs, audits).deleteArticleRecord(ACTOR, article.id());

        var order = inOrder(articles, jobs);
        order.verify(articles).softDeleteArticleRecord("tenant", article.id(), "admin", NOW);
        order.verify(jobs).softDeleteJobRecord("tenant", generation.id(), "admin", NOW);
        order.verify(jobs).softDeleteJobRecord("tenant", publication.id(), "admin", NOW);
    }

    @Test
    void shouldRejectArticleDeletionWhileLinkedJobIsActive() {
        ArticleRepository articles = mock(ArticleRepository.class);
        JobRepository jobs = mock(JobRepository.class);
        Article article = article();
        when(articles.findArticleById("tenant", article.id())).thenReturn(Optional.of(article));
        when(jobs.findByArticleReference("tenant", article.id())).thenReturn(List.of(job(UUID.randomUUID(),
                JobStatus.RUNNING, JobType.PUBLISH_ARTICLE,
                new JobPayload.PublishArticle(article.id(), UUID.randomUUID(), null), null)));

        assertThatThrownBy(() -> service(articles, jobs, mock(AuditRecorder.class))
                .deleteArticleRecord(ACTOR, article.id()))
                .isInstanceOfSatisfying(ApplicationException.class,
                        exception -> org.assertj.core.api.Assertions.assertThat(exception.code())
                                .isEqualTo("ARTICLE_DELETE_ACTIVE_JOB"));
        verify(articles, never()).softDeleteArticleRecord("tenant", article.id(), "admin", NOW);
    }

    @Test
    void shouldDeleteGeneratedArticleWhenDeletingGenerationJob() {
        ArticleRepository articles = mock(ArticleRepository.class);
        JobRepository jobs = mock(JobRepository.class);
        AuditRecorder audits = mock(AuditRecorder.class);
        Article article = article();
        Job generation = job(article.generationJobId(), JobStatus.SUCCEEDED, JobType.GENERATE_ARTICLE,
                new JobPayload.GenerateArticle(article.projectId(), null), article.id());
        when(jobs.findJobById("tenant", generation.id())).thenReturn(Optional.of(generation));
        when(articles.findByGenerationJobId("tenant", generation.id())).thenReturn(Optional.of(article));
        when(articles.findArticleById("tenant", article.id())).thenReturn(Optional.of(article));
        when(jobs.findByArticleReference("tenant", article.id())).thenReturn(List.of(generation));
        when(articles.softDeleteArticleRecord("tenant", article.id(), "admin", NOW)).thenReturn(true);
        when(jobs.softDeleteJobRecord("tenant", generation.id(), "admin", NOW)).thenReturn(true);

        service(articles, jobs, audits).deleteJobRecord(ACTOR, generation.id());

        verify(articles).softDeleteArticleRecord("tenant", article.id(), "admin", NOW);
        verify(jobs).softDeleteJobRecord("tenant", generation.id(), "admin", NOW);
    }

    @Test
    void shouldRejectActiveJobDeletion() {
        ArticleRepository articles = mock(ArticleRepository.class);
        JobRepository jobs = mock(JobRepository.class);
        Job active = job(UUID.randomUUID(), JobStatus.PENDING, JobType.IMPORT_PROJECT,
                new JobPayload.ImportProject("https://github.com/example/repo.git", null), null);
        when(jobs.findJobById("tenant", active.id())).thenReturn(Optional.of(active));

        assertThatThrownBy(() -> service(articles, jobs, mock(AuditRecorder.class))
                .deleteJobRecord(ACTOR, active.id()))
                .isInstanceOfSatisfying(ApplicationException.class,
                        exception -> org.assertj.core.api.Assertions.assertThat(exception.code())
                                .isEqualTo("JOB_DELETE_ACTIVE"));
        verify(jobs, never()).softDeleteJobRecord("tenant", active.id(), "admin", NOW);
    }

    @Test
    void shouldRestoreArticleAndLinkedJobsTogether() {
        ArticleRepository articles = mock(ArticleRepository.class);
        JobRepository jobs = mock(JobRepository.class);
        AuditRecorder audits = mock(AuditRecorder.class);
        Article article = article();
        Job generation = job(article.generationJobId(), JobStatus.SUCCEEDED, JobType.GENERATE_ARTICLE,
                new JobPayload.GenerateArticle(article.projectId(), null), article.id());
        Job publication = job(UUID.randomUUID(), JobStatus.FAILED, JobType.PUBLISH_ARTICLE,
                new JobPayload.PublishArticle(article.id(), UUID.randomUUID(), null), null);
        when(articles.findDeletedArticleById("tenant", article.id())).thenReturn(Optional.of(article));
        when(jobs.findDeletedByArticleReference("tenant", article.id())).thenReturn(List.of(generation, publication));
        when(articles.restoreArticleRecord("tenant", article.id())).thenReturn(true);
        when(jobs.restoreJobRecord("tenant", generation.id())).thenReturn(true);
        when(jobs.restoreJobRecord("tenant", publication.id())).thenReturn(true);

        service(articles, jobs, audits).restoreArticleRecord(ACTOR, article.id());

        var order = inOrder(articles, jobs);
        order.verify(articles).restoreArticleRecord("tenant", article.id());
        order.verify(jobs).restoreJobRecord("tenant", generation.id());
        order.verify(jobs).restoreJobRecord("tenant", publication.id());
    }

    private RecordManagementApplicationService service(ArticleRepository articles, JobRepository jobs,
                                                       AuditRecorder audits) {
        return new RecordManagementApplicationService(articles, jobs, audits, CLOCK);
    }

    private Article article() {
        return new Article(UUID.randomUUID(), "tenant", UUID.randomUUID(), UUID.randomUUID(), "待删除文章",
                "摘要", "正文", List.of("Java"), "zh-CN", "revision", 1, ArticleStatus.DRAFT,
                "admin", "admin", NOW, NOW);
    }

    private Job job(UUID id, JobStatus status, JobType type, JobPayload payload, UUID resultId) {
        return new Job(id, "tenant", "admin", type, status, payload, "job-key-001", "a".repeat(64),
                status == JobStatus.PENDING ? 0 : 1, 4, status.isActive() ? 20 : 100,
                "状态", "详情", null, NOW, null, null, resultId, null, null, NOW, NOW);
    }
}
