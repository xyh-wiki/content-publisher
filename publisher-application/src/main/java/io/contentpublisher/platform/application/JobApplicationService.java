package io.contentpublisher.platform.application;

import io.contentpublisher.platform.application.port.AuditRecorder;
import io.contentpublisher.platform.application.port.JobRepository;
import io.contentpublisher.platform.domain.ActorContext;
import io.contentpublisher.platform.domain.GenerationPolicy;
import io.contentpublisher.platform.domain.Job;
import io.contentpublisher.platform.domain.JobPayload;
import io.contentpublisher.platform.domain.JobStatus;
import io.contentpublisher.platform.domain.JobType;
import io.contentpublisher.platform.domain.ProjectStatus;
import io.contentpublisher.platform.domain.TopicBrief;
import io.contentpublisher.platform.domain.WebsiteBrief;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class JobApplicationService {
    private final JobRepository jobs;
    private final ProjectApplicationService projects;
    private final PublishingApplicationService publishing;
    private final AuditRecorder auditRecorder;
    private final Clock clock;
    private final int maxActiveJobsPerTenant;
    private final int maxAttempts;

    public JobApplicationService(JobRepository jobs, ProjectApplicationService projects,
                                 PublishingApplicationService publishing, AuditRecorder auditRecorder,
                                 Clock clock, int maxActiveJobsPerTenant, int maxAttempts) {
        this.jobs = jobs;
        this.projects = projects;
        this.publishing = publishing;
        this.auditRecorder = auditRecorder;
        this.clock = clock;
        this.maxActiveJobsPerTenant = maxActiveJobsPerTenant;
        this.maxAttempts = maxAttempts;
    }

    public Job submitImport(ActorContext actor, String gitUrl, String branch, String idempotencyKey) {
        validateGitUrlForStorage(gitUrl);
        JobPayload.ImportProject payload = new JobPayload.ImportProject(gitUrl, branch);
        return submit(actor, JobType.IMPORT_PROJECT, payload, idempotencyKey,
                hash("IMPORT_PROJECT", gitUrl, value(branch)));
    }

    public Job submitArticleGeneration(ActorContext actor, UUID projectId, GenerationPolicy policy,
                                       String idempotencyKey) {
        if (projects.getProject(actor, projectId).status() != ProjectStatus.READY) {
            throw new ApplicationException("PROJECT_NOT_READY", "项目尚未完成仓库分析");
        }
        JobPayload.GenerateArticle payload = new JobPayload.GenerateArticle(projectId, policy);
        return submit(actor, JobType.GENERATE_ARTICLE, payload, idempotencyKey,
                hash("GENERATE_ARTICLE", projectId.toString(), policy.toString()));
    }

    public Job submitTopicArticleGeneration(ActorContext actor, TopicBrief brief, GenerationPolicy policy,
                                            String idempotencyKey) {
        JobPayload.GenerateTopicArticle payload = new JobPayload.GenerateTopicArticle(brief, policy);
        return submit(actor, JobType.GENERATE_TOPIC_ARTICLE, payload, idempotencyKey,
                hash("GENERATE_TOPIC_ARTICLE", brief.toString(), policy.toString()));
    }

    public Job submitWebsiteArticleGeneration(ActorContext actor, WebsiteBrief brief, GenerationPolicy policy,
                                              String idempotencyKey) {
        validateWebsiteUrlForStorage(brief.websiteUrl());
        JobPayload.GenerateWebsiteArticle payload = new JobPayload.GenerateWebsiteArticle(brief, policy);
        return submit(actor, JobType.GENERATE_WEBSITE_ARTICLE, payload, idempotencyKey,
                hash("GENERATE_WEBSITE_ARTICLE", brief.toString(), policy.toString()));
    }

    public Job submitPublication(ActorContext actor, UUID articleId, UUID channelAccountId,
                                 String canonicalUrl, String idempotencyKey) {
        String normalizedCanonicalUrl = publishing.validateCanonicalUrl(canonicalUrl);
        publishing.assertPublishable(actor, articleId, channelAccountId);
        JobPayload.PublishArticle payload = new JobPayload.PublishArticle(articleId, channelAccountId, normalizedCanonicalUrl);
        return submit(actor, JobType.PUBLISH_ARTICLE, payload, idempotencyKey,
                hash("PUBLISH_ARTICLE", articleId.toString(), channelAccountId.toString(), value(normalizedCanonicalUrl)));
    }

    public List<Job> submitPublications(ActorContext actor, UUID articleId, List<UUID> channelAccountIds,
                                        String canonicalUrl, String idempotencyKey) {
        validateIdempotencyKey(idempotencyKey);
        List<UUID> accountIds = normalizeAccountIds(channelAccountIds);
        String normalizedCanonicalUrl = publishing.validateCanonicalUrl(canonicalUrl);
        accountIds.forEach(accountId -> publishing.assertPublishable(actor, articleId, accountId));

        LinkedHashMap<String, Job> resolved = new LinkedHashMap<>();
        List<Job> candidates = new java.util.ArrayList<>();
        Instant now = clock.instant();
        for (UUID accountId : accountIds) {
            String childKey = batchChildKey(idempotencyKey, accountId);
            String requestHash = hash("PUBLISH_ARTICLE", articleId.toString(), accountId.toString(),
                    value(normalizedCanonicalUrl));
            Job existing = existingJob(actor.tenantId(), JobType.PUBLISH_ARTICLE, childKey, requestHash);
            if (existing != null) {
                resolved.put(childKey, existing);
                continue;
            }
            JobPayload.PublishArticle payload = new JobPayload.PublishArticle(articleId, accountId,
                    normalizedCanonicalUrl);
            candidates.add(new Job(UUID.randomUUID(), actor.tenantId(), actor.subject(), JobType.PUBLISH_ARTICLE,
                    JobStatus.PENDING, payload, childKey, requestHash, 0, maxAttempts, now, null, null, null,
                    null, null, now, now));
        }

        List<Job> created = jobs.createBatchIfWithinQuota(candidates, maxActiveJobsPerTenant)
                .orElseThrow(() -> new ApplicationException("TENANT_JOB_QUOTA_EXCEEDED",
                        "租户活跃任务配额不足，无法提交全部发布平台"));
        created.forEach(job -> {
            resolved.put(job.idempotencyKey(), job);
            auditRecorder.record(actor, "JOB_SUBMITTED", "JOB", job.id(),
                    Map.of("jobType", job.type().name(), "idempotencyKey", job.idempotencyKey(),
                            "batchIdempotencyKey", idempotencyKey));
        });
        return accountIds.stream().map(accountId -> resolved.get(batchChildKey(idempotencyKey, accountId))).toList();
    }

    public Job getJob(ActorContext actor, UUID jobId) {
        return jobs.findJobById(actor.tenantId(), jobId)
                .orElseThrow(() -> new ApplicationException("JOB_NOT_FOUND", "任务不存在"));
    }

    public List<Job> listJobs(ActorContext actor, int limit) {
        if (limit < 1 || limit > 100) {
            throw new IllegalArgumentException("列表查询数量必须在 1 到 100 之间");
        }
        return jobs.findRecentJobs(actor.tenantId(), limit);
    }

    private Job submit(ActorContext actor, JobType type, JobPayload payload, String idempotencyKey, String requestHash) {
        validateIdempotencyKey(idempotencyKey);
        Job existing = existingJob(actor.tenantId(), type, idempotencyKey, requestHash);
        if (existing != null) return existing;
        Instant now = clock.instant();
        Job candidate = new Job(UUID.randomUUID(), actor.tenantId(), actor.subject(), type, JobStatus.PENDING,
                payload, idempotencyKey, requestHash, 0, maxAttempts, now, null, null, null,
                null, null, now, now);
        Job saved = jobs.createIfWithinQuota(candidate, maxActiveJobsPerTenant)
                .orElseThrow(() -> new ApplicationException("TENANT_JOB_QUOTA_EXCEEDED", "租户活跃任务数量已达到上限"));
        auditRecorder.record(actor, "JOB_SUBMITTED", "JOB", saved.id(),
                Map.of("jobType", type.name(), "idempotencyKey", idempotencyKey));
        return saved;
    }

    private Job existingJob(String tenantId, JobType type, String idempotencyKey, String requestHash) {
        Job existing = jobs.findByIdempotencyKey(tenantId, idempotencyKey).orElse(null);
        if (existing != null && (existing.type() != type || !existing.requestHash().equals(requestHash))) {
            throw new ApplicationException("IDEMPOTENCY_KEY_CONFLICT", "幂等键已用于不同请求");
        }
        return existing;
    }

    private List<UUID> normalizeAccountIds(List<UUID> accountIds) {
        if (accountIds == null) throw new ApplicationException("INVALID_ARGUMENT", "请至少选择一个发布账号");
        List<UUID> normalized = accountIds.stream().filter(java.util.Objects::nonNull).distinct().toList();
        if (normalized.isEmpty()) throw new ApplicationException("INVALID_ARGUMENT", "请至少选择一个发布账号");
        if (normalized.size() > 20) throw new ApplicationException("INVALID_ARGUMENT", "单次最多发布到 20 个账号");
        return normalized;
    }

    private String batchChildKey(String batchKey, UUID accountId) {
        return "publication-batch:" + hash(batchKey, accountId.toString()).substring(0, 48);
    }

    private void validateIdempotencyKey(String key) {
        if (key == null || !key.matches("[A-Za-z0-9._:-]{8,128}")) {
            throw new ApplicationException("IDEMPOTENCY_KEY_INVALID",
                    "Idempotency-Key 必须为 8 到 128 位字母、数字、点、下划线、冒号或连字符");
        }
    }

    private void validateGitUrlForStorage(String gitUrl) {
        try {
            java.net.URI uri = java.net.URI.create(gitUrl);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null
                    || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null) {
                throw new ApplicationException("GIT_URL_REJECTED",
                        "Git 地址必须为不含凭据、查询参数和片段的 HTTPS URL");
            }
        } catch (IllegalArgumentException exception) {
            throw new ApplicationException("GIT_URL_INVALID", "Git 地址格式无效", exception);
        }
    }

    private void validateWebsiteUrlForStorage(String websiteUrl) {
        try {
            java.net.URI uri = java.net.URI.create(websiteUrl);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null
                    || uri.getUserInfo() != null || uri.getFragment() != null) {
                throw new ApplicationException("WEBSITE_URL_REJECTED",
                        "网站链接必须为不含账号密码和片段的 HTTPS URL");
            }
        } catch (ApplicationException exception) {
            throw exception;
        } catch (IllegalArgumentException exception) {
            throw new ApplicationException("WEBSITE_URL_INVALID", "网站链接格式无效", exception);
        }
    }

    private String hash(String... values) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (String value : values) {
                digest.update(value.getBytes(StandardCharsets.UTF_8));
                digest.update((byte) 0x1f);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("运行环境缺少 SHA-256", exception);
        }
    }

    private String value(String value) {
        return value == null ? "" : value;
    }
}
