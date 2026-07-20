package io.contentpublisher.platform.web.form;

import io.contentpublisher.platform.domain.ContentFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ManualPublicationForm {
    @NotNull
    private ContentFormat contentFormat;

    @NotBlank(message = "平台标题不能为空")
    @Size(max = 500, message = "平台标题不能超过 500 个字符")
    private String title;

    @NotBlank(message = "平台发布内容不能为空")
    @Size(max = 20000, message = "平台发布内容不能超过 20000 个字符")
    private String content;

    @NotBlank(message = "请填写发布后的文章链接")
    @Size(max = 2048, message = "发布链接不能超过 2048 个字符")
    private String externalUrl;

    public ContentFormat getContentFormat() { return contentFormat; }
    public void setContentFormat(ContentFormat contentFormat) { this.contentFormat = contentFormat; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getExternalUrl() { return externalUrl; }
    public void setExternalUrl(String externalUrl) { this.externalUrl = externalUrl; }
}
