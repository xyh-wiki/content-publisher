package io.contentpublisher.platform.web.dto;

import io.contentpublisher.platform.domain.Article;
import io.contentpublisher.platform.domain.ArticleStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ArticleSeoViewTest {
    @Test
    void shouldScoreSearchOptimizedArticle() {
        String summary = "SEO 优化文章围绕搜索意图、主关键词、结构化标题和常见问题组织内容，帮助技术读者快速理解实施步骤、适用范围与注意事项。";
        String markdown = """
                SEO 优化需要先回答读者的搜索问题，再自然覆盖相关关键词和真实技术事实。

                ## SEO 标题与摘要优化

                ### 主关键词布局

                - 在标题、摘要和正文开头自然出现主关键词
                - 使用长尾问题词覆盖具体搜索场景

                ## 常见问题

                ### 是否需要固定关键词密度

                不需要，应优先保证自然表达和问题解决质量。

                参考[官方文档](https://example.com/docs)。
                """;
        Instant now = Instant.parse("2026-07-21T00:00:00Z");
        Article article = new Article(UUID.randomUUID(), "tenant", UUID.randomUUID(), null,
                "SEO 优化文章生成完整指南", summary, markdown, List.of("SEO 优化", "SEO 文章生成"),
                "zh-CN", "abc123", 1, ArticleStatus.DRAFT, "admin", "admin", now, now);

        ArticleSeoView seo = ArticleSeoView.from(article);

        assertThat(seo.score()).isEqualTo(100);
        assertThat(seo.grade()).isEqualTo("优秀");
        assertThat(seo.primaryKeyword()).isEqualTo("SEO 优化");
        assertThat(seo.checks()).allMatch(ArticleSeoView.Check::passed);
    }
}
