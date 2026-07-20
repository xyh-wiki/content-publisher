package io.contentpublisher.platform.domain;

import java.util.List;

public record TopicBrief(
        String topic,
        String description,
        String audience,
        String articleType,
        String knowledgeLevel,
        List<String> keywords,
        String referenceNotes) {
    public TopicBrief {
        topic = required(topic, "主题", 300);
        description = required(description, "主题描述", 4000);
        audience = required(audience, "目标受众", 500);
        articleType = required(articleType, "文章类型", 50);
        knowledgeLevel = required(knowledgeLevel, "知识层级", 30);
        keywords = keywords == null ? List.of() : keywords.stream().map(String::trim)
                .filter(value -> !value.isBlank()).distinct().limit(30).toList();
        referenceNotes = optional(referenceNotes, 10_000);
    }

    private static String required(String value, String label, int maxLength) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(label + "不能为空");
        String normalized = value.trim();
        if (normalized.length() > maxLength) throw new IllegalArgumentException(label + "长度超过限制");
        return normalized;
    }

    private static String optional(String value, int maxLength) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.trim();
        if (normalized.length() > maxLength) throw new IllegalArgumentException("参考资料长度超过限制");
        return normalized;
    }
}
