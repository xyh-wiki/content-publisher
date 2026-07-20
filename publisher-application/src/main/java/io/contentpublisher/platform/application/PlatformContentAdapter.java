package io.contentpublisher.platform.application;

import io.contentpublisher.platform.domain.AdaptedContent;
import io.contentpublisher.platform.domain.Article;
import io.contentpublisher.platform.domain.ChannelType;
import io.contentpublisher.platform.domain.ContentFormat;

import java.util.List;
import java.util.Locale;

public final class PlatformContentAdapter {
    public AdaptedContent adapt(Article article, ChannelType channelType, String canonicalUrl) {
        ChannelCatalog.ChannelDefinition definition = ChannelCatalog.definition(channelType);
        List<String> sourceTags = article.tags().isEmpty() ? article.keywords() : article.tags();
        List<String> tags = sourceTags.stream().map(this::normalizeTag)
                .filter(value -> !value.isBlank()).distinct().limit(tagLimit(channelType)).toList();
        String body = switch (definition.contentFormat()) {
            case MARKDOWN -> markdown(article, channelType, canonicalUrl);
            case PLAIN_TEXT -> plainText(article, channelType, canonicalUrl, tags);
            case SHORT_TEXT -> shortText(article, channelType, canonicalUrl, tags, definition.characterLimit());
        };
        return new AdaptedContent(channelType, definition.contentFormat(), adaptTitle(article.title(), channelType),
                truncate(body, definition.characterLimit()), tags, definition.characterLimit());
    }

    private String markdown(Article article, ChannelType channelType, String canonicalUrl) {
        String markdown = article.markdown();
        if (channelType == ChannelType.V2EX) {
            markdown = "## " + article.title() + "\n\n" + article.summary() + "\n\n" + markdown;
        }
        if (channelType == ChannelType.DEV || channelType == ChannelType.HASHNODE
                || channelType == ChannelType.MEDIUM || channelType == ChannelType.GHOST) {
            return markdown;
        }
        return appendLink(markdown, canonicalUrl, "原文链接");
    }

    private String plainText(Article article, ChannelType channelType, String canonicalUrl, List<String> tags) {
        String plain = stripMarkdown(article.markdown());
        StringBuilder content = new StringBuilder();
        if (channelType == ChannelType.XIAOHONGSHU) {
            content.append(article.summary()).append("\n\n").append(plain);
        } else {
            content.append(plain);
        }
        if (canonicalUrl != null) content.append("\n\n原文链接：").append(canonicalUrl);
        if (!tags.isEmpty()) {
            content.append("\n\n");
            tags.forEach(tag -> content.append('#').append(tag).append(' '));
        }
        return content.toString().strip();
    }

    private String shortText(Article article, ChannelType channelType, String canonicalUrl, List<String> tags,
                             int limit) {
        StringBuilder suffix = new StringBuilder();
        if (canonicalUrl != null) suffix.append("\n\n").append(canonicalUrl);
        if (!tags.isEmpty()) {
            suffix.append("\n");
            int allowedTags = channelType == ChannelType.X ? 2 : 3;
            tags.stream().limit(allowedTags).forEach(tag -> suffix.append('#').append(tag).append(' '));
        }
        String lead = article.title() + "\n\n" + article.summary();
        int available = Math.max(20, limit - codePoints(suffix.toString()));
        return truncate(lead, available) + suffix.toString().stripTrailing();
    }

    private String adaptTitle(String title, ChannelType type) {
        return switch (type) {
            case XIAOHONGSHU -> truncate(title, 20);
            case WECHAT_OFFICIAL -> truncate(title, 64);
            case TOUTIAO -> truncate(title, 30);
            case BILIBILI_COLUMN -> truncate(title, 80);
            case LINKEDIN, JIANSHU -> truncate(title, 100);
            case REDDIT -> truncate(title, 300);
            case X, MASTODON -> truncate(title, 100);
            default -> title;
        };
    }

    private String appendLink(String content, String canonicalUrl, String label) {
        return canonicalUrl == null ? content : content + "\n\n" + label + "：" + canonicalUrl;
    }

    private String stripMarkdown(String markdown) {
        return markdown
                .replaceAll("(?m)^```[^\\n]*$", "")
                .replace("```", "")
                .replaceAll("(?m)^#{1,6}\\s*", "")
                .replaceAll("(?m)^>\\s?", "")
                .replaceAll("(?m)^[-*+]\\s+", "• ")
                .replaceAll("(?m)^\\d+\\.\\s+", "")
                .replaceAll("!\\[([^]]*)]\\([^)]*\\)", "$1")
                .replaceAll("\\[([^]]+)]\\(([^)]+)\\)", "$1（$2）")
                .replaceAll("[*_~`]", "")
                .replaceAll("\\n{3,}", "\n\n")
                .strip();
    }

    private int tagLimit(ChannelType type) {
        return switch (type) {
            case DEV -> 4;
            case XIAOHONGSHU -> 10;
            case LINKEDIN -> 5;
            case X, MASTODON -> 3;
            default -> 8;
        };
    }

    private String normalizeTag(String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}_-]", "");
    }

    private String truncate(String value, int maxCodePoints) {
        if (codePoints(value) <= maxCodePoints) return value;
        int end = value.offsetByCodePoints(0, Math.max(1, maxCodePoints - 1));
        return value.substring(0, end).stripTrailing() + "…";
    }

    private int codePoints(String value) {
        return value.codePointCount(0, value.length());
    }
}
