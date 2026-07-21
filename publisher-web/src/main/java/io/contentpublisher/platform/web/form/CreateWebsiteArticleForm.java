package io.contentpublisher.platform.web.form;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CreateWebsiteArticleForm {
    @NotBlank @Size(max = 2048) @Pattern(regexp = "https://.+", message = "网站链接必须使用 HTTPS")
    private String websiteUrl;
    @NotBlank @Size(max = 2000) private String recommendationAngle = "总结网站定位、核心功能、适用人群、使用方式、优势与局限";
    @NotBlank @Size(max = 500) private String audience = "正在寻找相关工具或服务的技术用户";
    @Size(max = 3200) private String keywords = "";
    @NotBlank @Size(max = 20) private String language = "zh-CN";
    @NotBlank @Size(max = 50) private String tone = "客观、克制、信息密度高";
    @Min(200) @Max(3000) private int minCharacters = 600;
    @Min(200) @Max(3000) private int maxCharacters = 1800;
    @Min(1) @Max(30) private int maxKeywords = 12;
    @Size(max = 10000) private String excludedKeywords = "最好,第一,唯一,保证";
    @Size(max = 2200) private String requiredSections = "网站定位\n核心功能\n适用人群\n使用方式\n优势与局限\n总结";
    @NotBlank @Pattern(regexp = "[A-Za-z0-9._:-]{8,128}") private String idempotencyKey;

    public String getWebsiteUrl() { return websiteUrl; }
    public void setWebsiteUrl(String websiteUrl) { this.websiteUrl = websiteUrl; }
    public String getRecommendationAngle() { return recommendationAngle; }
    public void setRecommendationAngle(String recommendationAngle) { this.recommendationAngle = recommendationAngle; }
    public String getAudience() { return audience; }
    public void setAudience(String audience) { this.audience = audience; }
    public String getKeywords() { return keywords; }
    public void setKeywords(String keywords) { this.keywords = keywords; }
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
    public String getExcludedKeywords() { return excludedKeywords; }
    public void setExcludedKeywords(String excludedKeywords) { this.excludedKeywords = excludedKeywords; }
    public String getRequiredSections() { return requiredSections; }
    public void setRequiredSections(String requiredSections) { this.requiredSections = requiredSections; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
}
