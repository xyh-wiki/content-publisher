package io.contentpublisher.platform.application;

@FunctionalInterface
public interface JobProgressReporter {
    void update(int percent, String label, String detail);

    static JobProgressReporter noop() {
        return (percent, label, detail) -> { };
    }
}
