package io.contentpublisher.platform.infrastructure.channels;

import io.contentpublisher.platform.application.PlatformContentAdapter;
import io.contentpublisher.platform.domain.Article;
import io.contentpublisher.platform.domain.ArticleStatus;
import io.contentpublisher.platform.domain.ChannelType;
import io.contentpublisher.platform.domain.ContentOrigin;
import io.contentpublisher.platform.domain.ContentFormat;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PlatformContentAdapterTest {
    private final PlatformContentAdapter adapter = new PlatformContentAdapter();

    @Test
    void shouldKeepMarkdownForTechnicalCommunities() {
        var content = adapter.adapt(article(), ChannelType.JUEJIN, "https://example.com/source");

        assertThat(content.format()).isEqualTo(ContentFormat.MARKDOWN);
        assertThat(content.body()).contains("## 核心能力", "原文链接：https://example.com/source");
    }

    @Test
    void shouldCreatePlainTextAndHashtagsForXiaohongshu() {
        var content = adapter.adapt(article(), ChannelType.XIAOHONGSHU, null);

        assertThat(content.format()).isEqualTo(ContentFormat.PLAIN_TEXT);
        assertThat(content.title().codePointCount(0, content.title().length())).isLessThanOrEqualTo(20);
        assertThat(content.body()).doesNotContain("##", "**").contains("#java", "#内容分发");
    }

    @Test
    void shouldRespectTwitterUnicodeLimit() {
        Article source = article("技术平台".repeat(100), "能力摘要".repeat(100));

        var content = adapter.adapt(source, ChannelType.X, "https://example.com/source");

        assertThat(content.format()).isEqualTo(ContentFormat.SHORT_TEXT);
        assertThat(content.body().codePointCount(0, content.body().length())).isLessThanOrEqualTo(280);
        assertThat(content.body()).contains("https://example.com/source");
    }

    private Article article() {
        return article("企业级内容发布平台", "面向开发团队的多平台内容分发能力");
    }

    private Article article(String title, String summary) {
        Instant now = Instant.parse("2026-07-20T00:00:00Z");
        return new Article(UUID.randomUUID(), "tenant", ContentOrigin.git(UUID.randomUUID()), UUID.randomUUID(), title, summary,
                "## 核心能力\n\n支持 **Markdown** 与 [项目地址](https://example.com)。",
                List.of("Java", "内容分发"), List.of("Java 内容分发教程", "多平台发布方案"),
                "zh-CN", "a".repeat(40), 1, ArticleStatus.APPROVED,
                "editor", "admin", now, now);
    }
}
