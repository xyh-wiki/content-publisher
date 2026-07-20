package io.contentpublisher.platform.application;

import io.contentpublisher.platform.application.port.ArticleRepository;
import io.contentpublisher.platform.application.port.AuditRecorder;
import io.contentpublisher.platform.application.port.JobRepository;
import io.contentpublisher.platform.domain.ActorContext;
import io.contentpublisher.platform.domain.Article;
import io.contentpublisher.platform.domain.Job;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.time.Clock;

public final class RecordManagementApplicationService {
    private final ArticleRepository articles;
    private final JobRepository jobs;
    private final AuditRecorder auditRecorder;
    private final Clock clock;

    public RecordManagementApplicationService(ArticleRepository articles, JobRepository jobs,
                                              AuditRecorder auditRecorder, Clock clock) {
        this.articles = articles;
        this.jobs = jobs;
        this.auditRecorder = auditRecorder;
        this.clock = clock;
    }

    public void deleteArticleRecord(ActorContext actor, UUID articleId) {
        Article article = articles.findArticleById(actor.tenantId(), articleId)
                .orElseThrow(() -> new ApplicationException("ARTICLE_NOT_FOUND", "文章不存在"));
        List<Job> linkedJobs = jobs.findByArticleReference(actor.tenantId(), articleId);
        if (linkedJobs.stream().anyMatch(job -> job.status().isActive())) {
            throw new ApplicationException("ARTICLE_DELETE_ACTIVE_JOB",
                    "文章仍有关联任务正在执行，请等待任务结束后再删除整条记录");
        }
        var deletedAt = clock.instant();
        if (!articles.softDeleteArticleRecord(actor.tenantId(), articleId, actor.subject(), deletedAt)) {
            throw new ApplicationException("ARTICLE_NOT_FOUND", "文章不存在或已被删除");
        }
        linkedJobs.forEach(job -> jobs.softDeleteJobRecord(
                actor.tenantId(), job.id(), actor.subject(), deletedAt));
        auditRecorder.record(actor, "ARTICLE_RECORD_DELETED", "ARTICLE", articleId,
                Map.of("title", article.title(), "linkedJobs", Integer.toString(linkedJobs.size())));
    }

    public void deleteJobRecord(ActorContext actor, UUID jobId) {
        Job job = jobs.findJobById(actor.tenantId(), jobId)
                .orElseThrow(() -> new ApplicationException("JOB_NOT_FOUND", "任务不存在"));
        if (job.status().isActive()) {
            throw new ApplicationException("JOB_DELETE_ACTIVE", "运行中或等待执行的任务不能删除");
        }
        Article generatedArticle = articles.findByGenerationJobId(actor.tenantId(), jobId).orElse(null);
        if (generatedArticle != null) {
            deleteArticleRecord(actor, generatedArticle.id());
            auditRecorder.record(actor, "JOB_RECORD_DELETE_CASCADED", "JOB", jobId,
                    Map.of("articleId", generatedArticle.id().toString()));
            return;
        }
        if (!jobs.softDeleteJobRecord(actor.tenantId(), jobId, actor.subject(), clock.instant())) {
            throw new ApplicationException("JOB_NOT_FOUND", "任务不存在或已被删除");
        }
        auditRecorder.record(actor, "JOB_RECORD_DELETED", "JOB", jobId,
                Map.of("jobType", job.type().name(), "status", job.status().name()));
    }

    public List<DeletedRecord> listDeletedArticles(ActorContext actor, int limit) {
        return articles.findDeletedArticles(actor.tenantId(), requireLimit(limit));
    }

    public List<DeletedRecord> listDeletedJobs(ActorContext actor, int limit) {
        return jobs.findDeletedJobs(actor.tenantId(), requireLimit(limit));
    }

    public void restoreArticleRecord(ActorContext actor, UUID articleId) {
        Article article = articles.findDeletedArticleById(actor.tenantId(), articleId)
                .orElseThrow(() -> new ApplicationException("DELETED_ARTICLE_NOT_FOUND", "回收站中不存在该内容"));
        List<Job> linkedJobs = jobs.findDeletedByArticleReference(actor.tenantId(), articleId);
        if (!articles.restoreArticleRecord(actor.tenantId(), articleId)) {
            throw new ApplicationException("DELETED_ARTICLE_NOT_FOUND", "回收站内容已被恢复或清理");
        }
        linkedJobs.forEach(job -> jobs.restoreJobRecord(actor.tenantId(), job.id()));
        auditRecorder.record(actor, "ARTICLE_RECORD_RESTORED", "ARTICLE", articleId,
                Map.of("title", article.title(), "linkedJobs", Integer.toString(linkedJobs.size())));
    }

    public void restoreJobRecord(ActorContext actor, UUID jobId) {
        Job job = jobs.findDeletedJobById(actor.tenantId(), jobId)
                .orElseThrow(() -> new ApplicationException("DELETED_JOB_NOT_FOUND", "回收站中不存在该任务"));
        Article generatedArticle = articles.findDeletedByGenerationJobId(actor.tenantId(), jobId).orElse(null);
        if (generatedArticle != null) {
            restoreArticleRecord(actor, generatedArticle.id());
            return;
        }
        if (!jobs.restoreJobRecord(actor.tenantId(), jobId)) {
            throw new ApplicationException("DELETED_JOB_NOT_FOUND", "回收站任务已被恢复或清理");
        }
        auditRecorder.record(actor, "JOB_RECORD_RESTORED", "JOB", jobId,
                Map.of("jobType", job.type().name(), "status", job.status().name()));
    }

    private int requireLimit(int limit) {
        if (limit < 1 || limit > 100) throw new IllegalArgumentException("回收站查询数量必须在 1 到 100 之间");
        return limit;
    }
}
