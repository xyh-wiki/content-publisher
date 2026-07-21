package io.contentpublisher.platform.web.form;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class GenerateArticleForm {
    @NotBlank(message = "文章语言不能为空")
    @Size(max = 20, message = "文章语言不能超过 20 个字符")
    private String language = "zh-CN";

    @NotBlank(message = "文章语气不能为空")
    @Size(max = 50, message = "文章语气不能超过 50 个字符")
    private String tone = "专业、客观、面向开发者";

    @Min(value = 200, message = "最小字数不能少于 200")
    @Max(value = 3000, message = "最小字数不能超过 3000")
    private int minCharacters = 800;

    @Min(value = 200, message = "最大字数不能少于 200")
    @Max(value = 3000, message = "最大字数不能超过 3000")
    private int maxCharacters = 2200;

    @Min(value = 1, message = "关键词上限不能小于 1")
    @Max(value = 30, message = "关键词上限不能超过 30")
    private int maxKeywords = 12;

    @Size(max = 3200, message = "必选关键词内容过长")
    private String requiredKeywords = "";

    @Size(max = 10000, message = "禁用关键词内容过长")
    private String excludedKeywords = "";

    @Size(max = 2200, message = "章节要求内容过长")
    private String requiredSections = "项目概述\n核心能力\n快速开始";

    @NotBlank
    @Pattern(regexp = "[A-Za-z0-9._:-]{8,128}")
    private String idempotencyKey;

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public String getTone() { return tone; }
    public void setTone(String tone) { this.tone = tone; }
    public int getMinCharacters() { return minCharacters; }
    public void setMinCharacters(int minCharacters) { this.minCharacters = minCharacters; }
    public int getMaxCharacters() { return maxCharacters; }
    public void setMaxCharacters(int maxCharacters) { this.maxCharacters = maxCharacters; }
    public int getMaxKeywords() { return maxKeywords; }
    public void setMaxKeywords(int maxKeywords) { this.maxKeywords = maxKeywords; }
    public String getRequiredKeywords() { return requiredKeywords; }
    public void setRequiredKeywords(String requiredKeywords) { this.requiredKeywords = requiredKeywords; }
    public String getExcludedKeywords() { return excludedKeywords; }
    public void setExcludedKeywords(String excludedKeywords) { this.excludedKeywords = excludedKeywords; }
    public String getRequiredSections() { return requiredSections; }
    public void setRequiredSections(String requiredSections) { this.requiredSections = requiredSections; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
}
