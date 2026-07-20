package io.contentpublisher.platform.application;

import io.contentpublisher.platform.domain.ArticleSourceType;
import io.contentpublisher.platform.domain.ArticleStatus;
import io.contentpublisher.platform.domain.ChannelAccountStatus;
import io.contentpublisher.platform.domain.ChannelType;
import io.contentpublisher.platform.domain.JobStatus;
import io.contentpublisher.platform.domain.ProjectStatus;
import io.contentpublisher.platform.domain.PublicationStatus;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record MonitoringSnapshot(
        Instant capturedAt,
        Instant windowStart,
        MonitoringWindow window,
        long projectCount,
        long projectActivityCount,
        Map<ProjectStatus, Long> projectsByStatus,
        long articleCount,
        long articleActivityCount,
        Map<ArticleStatus, Long> articlesByStatus,
        Map<ArticleSourceType, Long> articlesBySource,
        long jobCount,
        long jobActivityCount,
        Map<JobStatus, Long> jobsByStatus,
        Map<JobStatus, Long> windowJobsByStatus,
        long publicationCount,
        long publicationActivityCount,
        Map<PublicationStatus, Long> publicationsByStatus,
        Map<PublicationStatus, Long> windowPublicationsByStatus,
        long accountCount,
        Map<ChannelAccountStatus, Long> accountsByStatus,
        long coveredChannelCount,
        List<ChannelPerformance> channelPerformance) {

    public MonitoringSnapshot {
        projectsByStatus = Map.copyOf(projectsByStatus);
        articlesByStatus = Map.copyOf(articlesByStatus);
        articlesBySource = Map.copyOf(articlesBySource);
        jobsByStatus = Map.copyOf(jobsByStatus);
        windowJobsByStatus = Map.copyOf(windowJobsByStatus);
        publicationsByStatus = Map.copyOf(publicationsByStatus);
        windowPublicationsByStatus = Map.copyOf(windowPublicationsByStatus);
        accountsByStatus = Map.copyOf(accountsByStatus);
        channelPerformance = List.copyOf(channelPerformance);
    }

    public record ChannelPerformance(ChannelType channelType, long total, long published, long failed) {
    }
}
