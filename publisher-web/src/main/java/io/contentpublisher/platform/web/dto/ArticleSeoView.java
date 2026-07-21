package io.contentpublisher.platform.web.dto;

import io.contentpublisher.platform.domain.Article;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public record ArticleSeoView(int score, String grade, String primaryKeyword, List<Check> checks) {
    public ArticleSeoView {
        checks = List.copyOf(checks);
    }

    public static ArticleSeoView from(Article article) {
        String primaryKeyword = article.keywords().stream().findFirst()
                .orElseGet(() -> article.tags().stream().findFirst().orElse(""));
        String title = article.title().trim();
        String summary = article.summary().trim();
        String markdown = article.markdown().trim();
        String opening = firstCodePoints(markdown, 400);
        int titleLength = codePoints(title);
        int summaryLength = codePoints(summary);
        int h2Count = countHeadings(markdown, 2);

        List<Check> checks = new ArrayList<>();
        add(checks, !primaryKeyword.isBlank(), 10, "主关键词", primaryKeyword.isBlank()
                ? "请在推荐关键词中设置主关键词" : "主关键词：" + primaryKeyword);
        add(checks, titleLength >= 8 && titleLength <= 40, 10, "SEO 标题长度",
                "当前 " + titleLength + " 字符，建议 8–40 字符");
        add(checks, containsIgnoreCase(title, primaryKeyword), 15, "标题包含主关键词",
                "主关键词应尽量靠近标题前部");
        add(checks, summaryLength >= 50 && summaryLength <= 200, 10, "Meta Description",
                "当前 " + summaryLength + " 字符，建议 50–200 字符");
        add(checks, containsIgnoreCase(summary + "\n" + opening, primaryKeyword), 15, "关键词前置",
                "主关键词应自然出现在摘要或正文开头");
        add(checks, h2Count >= 2, 15, "H2 内容层级",
                "当前 " + h2Count + " 个二级标题，建议至少 2 个");
        add(checks, hasScannableStructure(markdown), 10, "可扫描结构",
                "使用 H3、列表、表格或代码块拆解信息");
        add(checks, hasFaq(markdown), 10, "常见问题覆盖",
                "较长文章建议包含常见问题或 FAQ");
        add(checks, markdown.contains("]("), 5, "来源或延伸链接",
                "提供可信来源、官网或延伸阅读链接");

        int score = checks.stream().filter(Check::passed).mapToInt(Check::weight).sum();
        String grade = score >= 90 ? "优秀" : score >= 75 ? "良好" : score >= 60 ? "可优化" : "需完善";
        return new ArticleSeoView(score, grade, primaryKeyword, checks);
    }

    private static void add(List<Check> checks, boolean passed, int weight, String label, String detail) {
        checks.add(new Check(label, detail, passed, weight));
    }

    private static boolean containsIgnoreCase(String source, String value) {
        return value != null && !value.isBlank()
                && source.toLowerCase(Locale.ROOT).contains(value.toLowerCase(Locale.ROOT));
    }

    private static int countHeadings(String markdown, int level) {
        String prefix = "#".repeat(level) + " ";
        return (int) markdown.lines().map(String::stripLeading).filter(line -> line.startsWith(prefix)).count();
    }

    private static boolean hasScannableStructure(String markdown) {
        return countHeadings(markdown, 3) > 0 || markdown.lines().map(String::stripLeading)
                .anyMatch(line -> line.startsWith("- ") || line.matches("\\d+\\.\\s+.*")
                        || line.startsWith("|") || line.startsWith("```"));
    }

    private static boolean hasFaq(String markdown) {
        String normalized = markdown.toLowerCase(Locale.ROOT);
        return normalized.contains("## 常见问题") || normalized.contains("## faq")
                || normalized.contains("## frequently asked questions");
    }

    private static String firstCodePoints(String value, int maxCodePoints) {
        int count = codePoints(value);
        return count <= maxCodePoints ? value : value.substring(0, value.offsetByCodePoints(0, maxCodePoints));
    }

    private static int codePoints(String value) {
        return value.codePointCount(0, value.length());
    }

    public record Check(String label, String detail, boolean passed, int weight) {}
}
