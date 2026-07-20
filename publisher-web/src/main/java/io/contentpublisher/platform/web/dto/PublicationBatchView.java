package io.contentpublisher.platform.web.dto;

import io.contentpublisher.platform.domain.Job;
import io.contentpublisher.platform.domain.JobPayload;
import io.contentpublisher.platform.domain.JobStatus;
import io.contentpublisher.platform.domain.JobType;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record PublicationBatchView(
        UUID batchId,
        UUID articleId,
        String articleTitle,
        String status,
        String statusLabel,
        int progressPercent,
        int totalCount,
        int activeCount,
        int succeededCount,
        int failedCount,
        Instant createdAt,
        Instant updatedAt,
        List<Item> items) {

    public static List<PublicationBatchView> aggregate(List<Job> jobs, Map<UUID, String> articleNames,
                                                       Map<UUID, ChannelAccountView> accounts,
                                                       Map<io.contentpublisher.platform.domain.ChannelType, String> channelNames) {
        Map<UUID, List<Job>> grouped = new LinkedHashMap<>();
        jobs.stream()
                .filter(job -> job.type() == JobType.PUBLISH_ARTICLE && job.batchId() != null)
                .sorted(Comparator.comparing(Job::updatedAt).reversed())
                .forEach(job -> grouped.computeIfAbsent(job.batchId(), ignored -> new ArrayList<>()).add(job));
        return grouped.entrySet().stream().map(entry -> from(entry.getKey(), entry.getValue(), articleNames,
                        accounts, channelNames))
                .sorted(Comparator.comparing(PublicationBatchView::updatedAt).reversed())
                .limit(20)
                .toList();
    }

    private static PublicationBatchView from(UUID batchId, List<Job> jobs, Map<UUID, String> articleNames,
                                             Map<UUID, ChannelAccountView> accounts,
                                             Map<io.contentpublisher.platform.domain.ChannelType, String> channelNames) {
        Map<UUID, Job> latestByAccount = new LinkedHashMap<>();
        jobs.stream().sorted(Comparator.comparing(Job::updatedAt).reversed()).forEach(job -> {
            JobPayload.PublishArticle payload = (JobPayload.PublishArticle) job.payload();
            latestByAccount.putIfAbsent(payload.channelAccountId(), job);
        });
        List<Item> items = latestByAccount.values().stream().map(job -> {
                    JobPayload.PublishArticle payload = (JobPayload.PublishArticle) job.payload();
                    ChannelAccountView account = accounts.get(payload.channelAccountId());
                    JobProgressView progress = JobProgressView.from(job);
                    return new Item(job.id(), payload.channelAccountId(),
                            account == null ? payload.channelAccountId().toString() : account.displayName(),
                            account == null ? "未知渠道" : channelNames.getOrDefault(account.type(), account.type().name()),
                            job.status(), progress.percent(), progress.label(), job.errorCode(), job.errorMessage());
                })
                .sorted(Comparator.comparing(Item::accountName))
                .toList();
        int active = (int) items.stream().filter(item -> item.status().isActive()).count();
        int succeeded = (int) items.stream().filter(item -> item.status() == JobStatus.SUCCEEDED).count();
        int failed = (int) items.stream().filter(item -> item.status() == JobStatus.FAILED).count();
        int percent = items.isEmpty() ? 0 : (int) Math.round(items.stream()
                .mapToInt(Item::progressPercent).average().orElse(0));
        JobPayload.PublishArticle firstPayload = (JobPayload.PublishArticle) jobs.get(0).payload();
        String status = active > 0 ? "RUNNING" : failed > 0 ? "FAILED" : "SUCCEEDED";
        String statusLabel = active > 0 ? "发布中" : failed > 0
                ? (succeeded > 0 ? "部分失败" : "发布失败") : "全部完成";
        Instant createdAt = jobs.stream().map(Job::createdAt).min(Instant::compareTo).orElseThrow();
        Instant updatedAt = jobs.stream().map(Job::updatedAt).max(Instant::compareTo).orElseThrow();
        return new PublicationBatchView(batchId, firstPayload.articleId(),
                articleNames.getOrDefault(firstPayload.articleId(), firstPayload.articleId().toString()), status,
                statusLabel, percent, items.size(), active, succeeded, failed, createdAt, updatedAt, items);
    }

    public record Item(UUID jobId, UUID channelAccountId, String accountName, String channelName,
                       JobStatus status, int progressPercent, String progressLabel,
                       String errorCode, String errorMessage) {
        public boolean retryable() {
            return status == JobStatus.FAILED;
        }

        public String retryIdempotencyKey() {
            return "publication-retry:" + jobId;
        }
    }
}
