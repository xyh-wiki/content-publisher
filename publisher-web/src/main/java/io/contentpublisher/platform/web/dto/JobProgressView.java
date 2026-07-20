package io.contentpublisher.platform.web.dto;

import io.contentpublisher.platform.domain.Job;

public record JobProgressView(int percent, String label, String detail) {
    public static JobProgressView from(Job job) {
        return switch (job.status()) {
            case PENDING -> new JobProgressView(8, "等待执行", "任务已进入队列，正在等待后台工作器领取");
            case RETRY_WAIT -> new JobProgressView(35, "等待重试",
                    "上一次执行未完成，将按照重试策略再次处理");
            case SUCCEEDED -> new JobProgressView(100, "执行完成", "结果已经保存，可以继续下一步操作");
            case FAILED -> new JobProgressView(100, "执行失败", "任务已经停止，请根据异常信息调整后重试");
            case RUNNING -> running(job);
        };
    }

    private static JobProgressView running(Job job) {
        return switch (job.type()) {
            case IMPORT_PROJECT -> new JobProgressView(52, "正在分析仓库",
                    "正在拉取代码、读取 README、识别语言和生成仓库快照");
            case GENERATE_ARTICLE -> new JobProgressView(68, "正在生成文章",
                    "正在依据仓库事实生成正文、发布标签和推荐关键词");
            case GENERATE_TOPIC_ARTICLE -> new JobProgressView(68, "正在生成主题文章",
                    "正在按照创作简报组织教程结构并生成内容");
            case GENERATE_WEBSITE_ARTICLE -> new JobProgressView(62, "正在分析网站并生成文章",
                    "正在提取公开页面信息、生成推荐内容并补充官方网站链接");
            case PUBLISH_ARTICLE -> new JobProgressView(72, "正在发布内容",
                    "正在适配目标平台格式并调用渠道接口");
        };
    }
}
