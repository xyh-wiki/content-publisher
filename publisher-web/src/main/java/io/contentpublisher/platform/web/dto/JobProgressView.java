package io.contentpublisher.platform.web.dto;

import io.contentpublisher.platform.domain.Job;

public record JobProgressView(int percent, String label, String detail) {
    public static JobProgressView from(Job job) {
        return switch (job.status()) {
            case PENDING, RUNNING, RETRY_WAIT -> persisted(job);
            case SUCCEEDED -> new JobProgressView(100, "执行完成", "结果已经保存，可以继续下一步操作");
            case FAILED -> new JobProgressView(100, "执行失败", "任务已经停止，请根据异常信息调整后重试");
        };
    }

    private static JobProgressView persisted(Job job) {
        String label = job.progressLabel().isBlank() ? "处理中" : job.progressLabel();
        String detail = job.progressDetail().isBlank() ? "后台任务正在执行" : job.progressDetail();
        return new JobProgressView(job.progressPercent(), label, detail);
    }
}
