package io.contentpublisher.platform.infrastructure.channels;

import io.contentpublisher.platform.domain.Article;

final class ChannelContentFormatter {
    private ChannelContentFormatter() {}

    static String promotion(Article article, String canonicalUrl, int maxCodePoints) {
        String text = article.title() + "\n\n" + article.summary();
        if (canonicalUrl == null) return truncate(text, maxCodePoints);
        int urlPoints = canonicalUrl.codePointCount(0, canonicalUrl.length()) + 2;
        if (urlPoints >= maxCodePoints) return canonicalUrl;
        return truncate(text, maxCodePoints - urlPoints) + "\n\n" + canonicalUrl;
    }

    static String promotionForX(Article article, String canonicalUrl) {
        String text = article.title() + "\n\n" + article.summary();
        if (canonicalUrl == null) return truncate(text, 140);
        return truncate(text, 127) + "\n\n" + canonicalUrl;
    }

    static String articleWithLink(Article article, String canonicalUrl) {
        return canonicalUrl == null ? article.markdown()
                : article.markdown() + "\n\n原文链接：" + canonicalUrl;
    }

    static String truncate(String value, int maxCodePoints) {
        int count = value.codePointCount(0, value.length());
        if (count <= maxCodePoints) return value;
        int end = value.offsetByCodePoints(0, Math.max(maxCodePoints - 1, 1));
        return value.substring(0, end).stripTrailing() + "…";
    }
}
