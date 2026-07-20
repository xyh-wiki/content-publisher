package io.contentpublisher.platform.web.form;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UpdateArticleForm {
    @Min(value = 1, message = "文章版本无效")
    private int expectedVersion;

    @NotBlank(message = "标题不能为空")
    @Size(max = 500, message = "标题不能超过 500 个字符")
    private String title;

    @NotBlank(message = "摘要不能为空")
    @Size(max = 2000, message = "摘要不能超过 2000 个字符")
    private String summary;

    @NotBlank(message = "正文不能为空")
    @Size(max = 20000, message = "正文不能超过 20000 个字符")
    private String markdown;

    @Size(max = 3200, message = "关键词内容过长")
    private String keywords;

    @Size(max = 1000, message = "标签内容过长")
    private String tags;

    public int getExpectedVersion() { return expectedVersion; }
    public void setExpectedVersion(int expectedVersion) { this.expectedVersion = expectedVersion; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getMarkdown() { return markdown; }
    public void setMarkdown(String markdown) { this.markdown = markdown; }
    public String getKeywords() { return keywords; }
    public void setKeywords(String keywords) { this.keywords = keywords; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
}
