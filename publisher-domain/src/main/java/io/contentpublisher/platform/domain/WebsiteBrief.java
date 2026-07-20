package io.contentpublisher.platform.domain;

import java.util.List;

public record WebsiteBrief(
        String websiteUrl,
        String recommendationAngle,
        String audience,
        List<String> keywords) {
    public WebsiteBrief {
        if (websiteUrl == null || websiteUrl.isBlank() || websiteUrl.length() > 2048) {
            throw new IllegalArgumentException("网站链接不能为空且不能超过 2048 个字符");
        }
        websiteUrl = websiteUrl.trim();
        recommendationAngle = required(recommendationAngle, "推荐角度", 2000);
        audience = required(audience, "目标受众", 500);
        keywords = keywords == null ? List.of() : keywords.stream().map(String::trim)
                .filter(value -> !value.isBlank()).distinct().limit(30).toList();
    }

    private static String required(String value, String label, int maxLength) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(label + "不能为空");
        String normalized = value.trim();
        if (normalized.length() > maxLength) throw new IllegalArgumentException(label + "长度超过限制");
        return normalized;
    }
}
