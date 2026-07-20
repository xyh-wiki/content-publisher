package io.contentpublisher.platform.web.controller;

import io.contentpublisher.platform.application.ChannelCatalog;
import io.contentpublisher.platform.application.JobApplicationService;
import io.contentpublisher.platform.application.ProjectApplicationService;
import io.contentpublisher.platform.application.PublicationRecord;
import io.contentpublisher.platform.application.PublishingApplicationService;
import io.contentpublisher.platform.domain.ArticleSourceType;
import io.contentpublisher.platform.domain.ArticleStatus;
import io.contentpublisher.platform.domain.ChannelAccountStatus;
import io.contentpublisher.platform.domain.ChannelType;
import io.contentpublisher.platform.domain.JobStatus;
import io.contentpublisher.platform.domain.ProjectStatus;
import io.contentpublisher.platform.domain.PublicationStatus;
import io.contentpublisher.platform.web.security.RequestActorProvider;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
public class PortalMonitoringController {
    private static final int MONITORING_SAMPLE_LIMIT = 100;

    private final ProjectApplicationService projects;
    private final JobApplicationService jobs;
    private final PublishingApplicationService publishing;
    private final RequestActorProvider actors;
    private final Clock clock;

    public PortalMonitoringController(ProjectApplicationService projects,
                                      JobApplicationService jobs,
                                      PublishingApplicationService publishing,
                                      RequestActorProvider actors,
                                      Clock clock) {
        this.projects = projects;
        this.jobs = jobs;
        this.publishing = publishing;
        this.actors = actors;
        this.clock = clock;
    }

    @GetMapping("/monitoring")
    public String monitoring(Model model) {
        var actor = actors.currentActor();
        var projectList = projects.listProjects(actor, MONITORING_SAMPLE_LIMIT);
        var articleList = projects.listArticles(actor, MONITORING_SAMPLE_LIMIT);
        var jobList = jobs.listJobs(actor, MONITORING_SAMPLE_LIMIT);
        var publicationList = publishing.listPublicationRecords(actor, MONITORING_SAMPLE_LIMIT);
        var accountList = publishing.listAccounts(actor);
        Instant capturedAt = clock.instant();
        Instant last24Hours = capturedAt.minus(24, ChronoUnit.HOURS);

        long readyProjects = count(projectList, project -> project.status() == ProjectStatus.READY);
        long activeJobs = count(jobList, job -> job.status().isActive());
        long succeededJobs = count(jobList, job -> job.status() == JobStatus.SUCCEEDED);
        long failedJobs = count(jobList, job -> job.status() == JobStatus.FAILED);
        long finishedJobs = succeededJobs + failedJobs;
        long publishedRecords = count(publicationList, record -> record.status() == PublicationStatus.PUBLISHED);
        long failedPublications = count(publicationList, record -> record.status() == PublicationStatus.FAILED);
        long completedPublications = publishedRecords + failedPublications;
        long activeAccounts = count(accountList, account -> account.status() == ChannelAccountStatus.ACTIVE);
        long publishableArticles = count(articleList, article -> article.status() == ArticleStatus.APPROVED
                || article.status() == ArticleStatus.PUBLISHED);

        model.addAttribute("capturedAt", capturedAt);
        model.addAttribute("sampleLimit", MONITORING_SAMPLE_LIMIT);
        model.addAttribute("projectCount", projectList.size());
        model.addAttribute("articleCount", articleList.size());
        model.addAttribute("activeJobCount", activeJobs);
        model.addAttribute("publicationCount", publicationList.size());
        model.addAttribute("projects24h", count(projectList, project -> isRecent(project.updatedAt(), last24Hours)));
        model.addAttribute("articles24h", count(articleList, article -> isRecent(article.updatedAt(), last24Hours)));
        model.addAttribute("jobs24h", count(jobList, job -> isRecent(job.updatedAt(), last24Hours)));
        model.addAttribute("publications24h", count(publicationList,
                record -> isRecent(record.updatedAt(), last24Hours)));
        model.addAttribute("readyProjectRate", percentage(readyProjects, projectList.size()));
        model.addAttribute("jobSuccessRate", percentage(succeededJobs, finishedJobs));
        model.addAttribute("publicationSuccessRate", percentage(publishedRecords, completedPublications));
        model.addAttribute("activeAccountRate", percentage(activeAccounts, accountList.size()));
        model.addAttribute("activeAccountCount", activeAccounts);
        model.addAttribute("accountCount", accountList.size());
        model.addAttribute("publishableArticleCount", publishableArticles);
        model.addAttribute("coveredChannelCount", publicationList.stream()
                .filter(record -> record.status() == PublicationStatus.PUBLISHED)
                .map(PublicationRecord::channelType).distinct().count());
        model.addAttribute("supportedChannelCount", ChannelCatalog.all().size());
        model.addAttribute("failedJobCount", failedJobs);
        model.addAttribute("failedPublicationCount", failedPublications);
        model.addAttribute("disabledAccountCount", accountList.size() - activeAccounts);
        model.addAttribute("systemStatus", systemStatus(activeJobs, failedJobs, failedPublications));
        model.addAttribute("systemStatusTone", systemStatusTone(failedJobs, failedPublications));
        model.addAttribute("projectStatus", projectStatus(projectList));
        model.addAttribute("articleStatus", articleStatus(articleList));
        model.addAttribute("jobStatus", jobStatus(jobList));
        model.addAttribute("publicationStatus", publicationStatus(publicationList));
        model.addAttribute("sourceStatus", sourceStatus(articleList));
        model.addAttribute("channelPerformance", channelPerformance(publicationList));
        model.addAttribute("recentJobs", jobList.stream().limit(8).toList());
        model.addAttribute("recentPublications", publicationList.stream().limit(8).toList());
        model.addAttribute("articleNames", articleNames(articleList));
        model.addAttribute("channelNames", channelNames());
        model.addAttribute("alerts", alerts(failedJobs, failedPublications,
                accountList.size() - activeAccounts, activeJobs));
        return "monitoring";
    }

    private List<MonitorBar> projectStatus(List<io.contentpublisher.platform.domain.Project> projects) {
        return List.of(
                bar("就绪", count(projects, item -> item.status() == ProjectStatus.READY), projects.size(), "success"),
                bar("分析中", count(projects, item -> item.status() == ProjectStatus.ANALYZING), projects.size(), "primary"),
                bar("失败", count(projects, item -> item.status() == ProjectStatus.FAILED), projects.size(), "danger")
        );
    }

    private List<MonitorBar> articleStatus(List<io.contentpublisher.platform.domain.Article> articles) {
        return List.of(
                bar("草稿", count(articles, item -> item.status() == ArticleStatus.DRAFT), articles.size(), "warning"),
                bar("已审核", count(articles, item -> item.status() == ArticleStatus.APPROVED), articles.size(), "primary"),
                bar("已发布", count(articles, item -> item.status() == ArticleStatus.PUBLISHED), articles.size(), "success"),
                bar("已驳回", count(articles, item -> item.status() == ArticleStatus.REJECTED), articles.size(), "danger")
        );
    }

    private List<MonitorBar> jobStatus(List<io.contentpublisher.platform.domain.Job> jobs) {
        return List.of(
                bar("等待", count(jobs, item -> item.status() == JobStatus.PENDING), jobs.size(), "muted"),
                bar("运行中", count(jobs, item -> item.status() == JobStatus.RUNNING), jobs.size(), "primary"),
                bar("重试等待", count(jobs, item -> item.status() == JobStatus.RETRY_WAIT), jobs.size(), "warning"),
                bar("成功", count(jobs, item -> item.status() == JobStatus.SUCCEEDED), jobs.size(), "success"),
                bar("失败", count(jobs, item -> item.status() == JobStatus.FAILED), jobs.size(), "danger")
        );
    }

    private List<MonitorBar> publicationStatus(List<PublicationRecord> records) {
        return List.of(
                bar("发布中", count(records, item -> item.status() == PublicationStatus.PUBLISHING),
                        records.size(), "primary"),
                bar("发布成功", count(records, item -> item.status() == PublicationStatus.PUBLISHED),
                        records.size(), "success"),
                bar("发布失败", count(records, item -> item.status() == PublicationStatus.FAILED),
                        records.size(), "danger")
        );
    }

    private List<MonitorBar> sourceStatus(List<io.contentpublisher.platform.domain.Article> articles) {
        return List.of(
                bar("Git 项目", count(articles, item -> item.sourceType() == ArticleSourceType.GIT),
                        articles.size(), "primary"),
                bar("主题教程", count(articles, item -> item.sourceType() == ArticleSourceType.TOPIC),
                        articles.size(), "success"),
                bar("网站推荐", count(articles, item -> item.sourceType() == ArticleSourceType.WEBSITE),
                        articles.size(), "warning")
        );
    }

    private List<ChannelMetric> channelPerformance(List<PublicationRecord> records) {
        Map<ChannelType, List<PublicationRecord>> grouped = new EnumMap<>(ChannelType.class);
        records.forEach(record -> grouped.computeIfAbsent(record.channelType(), ignored -> new ArrayList<>()).add(record));
        return grouped.entrySet().stream().map(entry -> {
            long published = count(entry.getValue(), item -> item.status() == PublicationStatus.PUBLISHED);
            long failed = count(entry.getValue(), item -> item.status() == PublicationStatus.FAILED);
            return new ChannelMetric(entry.getKey(), ChannelCatalog.definition(entry.getKey()).displayName(),
                    entry.getValue().size(), published, failed, percentage(published, published + failed));
        }).sorted(Comparator.comparingLong(ChannelMetric::total).reversed()
                .thenComparing(ChannelMetric::displayName)).limit(8).toList();
    }

    private List<MonitorAlert> alerts(long failedJobs, long failedPublications, long disabledAccounts,
                                      long activeJobs) {
        List<MonitorAlert> alerts = new ArrayList<>();
        if (failedJobs > 0) alerts.add(new MonitorAlert("danger", "后台任务失败", failedJobs,
                "检查失败原因并决定是否重新发起任务", "/projects"));
        if (failedPublications > 0) alerts.add(new MonitorAlert("danger", "发布记录失败", failedPublications,
                "核对渠道凭据、接口返回和内容限制", "/publishing"));
        if (disabledAccounts > 0) alerts.add(new MonitorAlert("warning", "渠道账号停用", disabledAccounts,
                "停用账号不会接收新的自动发布任务", "/channels"));
        if (activeJobs > 0) alerts.add(new MonitorAlert("primary", "任务正在处理", activeJobs,
                "队列正在推进，页面会自动刷新", "/projects"));
        if (alerts.isEmpty()) alerts.add(new MonitorAlert("success", "当前无运行异常", 0,
                "任务、渠道与发布链路均未发现待处理告警", "/monitoring"));
        return alerts;
    }

    private Map<UUID, String> articleNames(List<io.contentpublisher.platform.domain.Article> articles) {
        Map<UUID, String> names = new java.util.HashMap<>();
        articles.forEach(article -> names.put(article.id(), article.title()));
        return names;
    }

    private Map<ChannelType, String> channelNames() {
        Map<ChannelType, String> names = new EnumMap<>(ChannelType.class);
        ChannelCatalog.all().forEach(channel -> names.put(channel.type(), channel.displayName()));
        return names;
    }

    private String systemStatus(long activeJobs, long failedJobs, long failedPublications) {
        if (failedJobs + failedPublications > 0) return "需要关注";
        if (activeJobs > 0) return "任务处理中";
        return "运行正常";
    }

    private String systemStatusTone(long failedJobs, long failedPublications) {
        return failedJobs + failedPublications > 0 ? "warning" : "success";
    }

    private MonitorBar bar(String label, long value, long total, String tone) {
        return new MonitorBar(label, value, Math.max(total, 1), percentage(value, total), tone);
    }

    private int percentage(long value, long total) {
        if (total <= 0) return 0;
        return (int) Math.round(value * 100.0 / total);
    }

    private boolean isRecent(Instant value, Instant threshold) {
        return value != null && !value.isBefore(threshold);
    }

    private <T> long count(List<T> items, java.util.function.Predicate<T> predicate) {
        return items.stream().filter(predicate).count();
    }

    public record MonitorBar(String label, long value, long total, int percentage, String tone) {
    }

    public record ChannelMetric(ChannelType type, String displayName, long total, long published, long failed,
                                int successRate) {
    }

    public record MonitorAlert(String tone, String title, long count, String description, String link) {
    }
}
