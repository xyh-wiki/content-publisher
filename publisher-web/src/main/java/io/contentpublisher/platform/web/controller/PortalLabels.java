package io.contentpublisher.platform.web.controller;

import io.contentpublisher.platform.domain.JobStatus;
import io.contentpublisher.platform.domain.JobType;

import java.util.EnumMap;
import java.util.Map;

final class PortalLabels {
    private static final Map<JobType, String> JOB_TYPES = jobTypes();
    private static final Map<JobStatus, String> JOB_STATUSES = jobStatuses();

    private PortalLabels() {
    }

    static Map<JobType, String> jobTypeNames() {
        return JOB_TYPES;
    }

    static Map<JobStatus, String> jobStatusNames() {
        return JOB_STATUSES;
    }

    private static Map<JobType, String> jobTypes() {
        var names = new EnumMap<JobType, String>(JobType.class);
        names.put(JobType.IMPORT_PROJECT, "导入 Git 项目");
        names.put(JobType.GENERATE_ARTICLE, "生成项目文章");
        names.put(JobType.GENERATE_TOPIC_ARTICLE, "生成主题教程");
        names.put(JobType.GENERATE_WEBSITE_ARTICLE, "生成网站推荐");
        names.put(JobType.PUBLISH_ARTICLE, "发布文章");
        return Map.copyOf(names);
    }

    private static Map<JobStatus, String> jobStatuses() {
        var names = new EnumMap<JobStatus, String>(JobStatus.class);
        names.put(JobStatus.PENDING, "等待执行");
        names.put(JobStatus.RUNNING, "执行中");
        names.put(JobStatus.RETRY_WAIT, "等待重试");
        names.put(JobStatus.SUCCEEDED, "执行成功");
        names.put(JobStatus.FAILED, "执行失败");
        return Map.copyOf(names);
    }
}
