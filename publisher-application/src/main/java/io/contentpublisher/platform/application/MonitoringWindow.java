package io.contentpublisher.platform.application;

import java.time.Duration;
import java.util.Arrays;

public enum MonitoringWindow {
    HOURS_24("24h", "近 24 小时", Duration.ofHours(24)),
    DAYS_7("7d", "近 7 天", Duration.ofDays(7)),
    DAYS_30("30d", "近 30 天", Duration.ofDays(30));

    private final String code;
    private final String label;
    private final Duration duration;

    MonitoringWindow(String code, String label, Duration duration) {
        this.code = code;
        this.label = label;
        this.duration = duration;
    }

    public String code() {
        return code;
    }

    public String label() {
        return label;
    }

    public Duration duration() {
        return duration;
    }

    public static MonitoringWindow fromCode(String value) {
        if (value == null || value.isBlank()) return HOURS_24;
        return Arrays.stream(values()).filter(window -> window.code.equalsIgnoreCase(value.trim()))
                .findFirst().orElse(HOURS_24);
    }
}
