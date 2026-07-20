package io.contentpublisher.platform.domain;

public enum JobStatus {
    PENDING,
    RUNNING,
    RETRY_WAIT,
    SUCCEEDED,
    FAILED;

    public boolean isActive() {
        return this == PENDING || this == RUNNING || this == RETRY_WAIT;
    }
}
