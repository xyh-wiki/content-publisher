package io.contentpublisher.platform.domain;

import java.util.List;

public record GenerationPolicy(
        String language,
        String tone,
        int minCharacters,
        int maxCharacters,
        int maxKeywords,
        List<String> requiredKeywords,
        List<String> excludedKeywords,
        List<String> requiredSections) {
    public static final int MAX_CHARACTERS = 3_000;

    public GenerationPolicy {
        requiredKeywords = normalized(requiredKeywords);
        excludedKeywords = normalized(excludedKeywords);
        requiredSections = normalized(requiredSections);
        if (minCharacters < 200 || maxCharacters > MAX_CHARACTERS || minCharacters > maxCharacters) {
            throw new IllegalArgumentException("文章长度约束无效");
        }
        if (maxKeywords < 1 || maxKeywords > 30) {
            throw new IllegalArgumentException("关键词数量必须在 1 到 30 之间");
        }
        if (requiredKeywords.size() > maxKeywords) {
            throw new IllegalArgumentException("必选关键词数量不能超过关键词上限");
        }
        if (requiredKeywords.stream().anyMatch(excludedKeywords::contains)) {
            throw new IllegalArgumentException("关键词不能同时属于必选和禁用集合");
        }
    }

    private static List<String> normalized(List<String> values) {
        return values == null ? List.of() : values.stream()
                .map(String::trim).filter(value -> !value.isBlank()).distinct().toList();
    }
}
