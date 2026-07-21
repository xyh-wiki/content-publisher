package io.contentpublisher.platform.web.form;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CreateTopicArticleForm {
    @NotBlank(message = "创作主题不能为空") @Size(max = 300) private String topic;
    @NotBlank(message = "主题描述不能为空") @Size(max = 4000) private String description;
    @NotBlank(message = "目标受众不能为空") @Size(max = 500) private String audience = "希望系统学习该主题的开发者";
    @Pattern(regexp = "TUTORIAL|KNOWLEDGE_GUIDE|BEST_PRACTICES|TROUBLESHOOTING|CONCEPT_EXPLAINER")
    private String articleType = "TUTORIAL";
    @Pattern(regexp = "BEGINNER|INTERMEDIATE|ADVANCED|MIXED") private String knowledgeLevel = "MIXED";
    @Size(max = 3200) private String keywords = "";
    @Size(max = 10000) private String referenceNotes = "";
    @NotBlank @Size(max = 20) private String language = "zh-CN";
    @NotBlank @Size(max = 50) private String tone = "专业、清晰、循序渐进";
    @Min(200) @Max(3000) private int minCharacters = 800;
    @Min(200) @Max(3000) private int maxCharacters = 2200;
    @Min(1) @Max(30) private int maxKeywords = 12;
    @Size(max = 10000) private String excludedKeywords = "";
    @Size(max = 2200) private String requiredSections = "学习目标\n前置知识\n分步教程\n完整示例\n常见问题\n总结";
    @NotBlank @Pattern(regexp = "[A-Za-z0-9._:-]{8,128}") private String idempotencyKey;

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getAudience() { return audience; }
    public void setAudience(String audience) { this.audience = audience; }
    public String getArticleType() { return articleType; }
    public void setArticleType(String articleType) { this.articleType = articleType; }
    public String getKnowledgeLevel() { return knowledgeLevel; }
    public void setKnowledgeLevel(String knowledgeLevel) { this.knowledgeLevel = knowledgeLevel; }
    public String getKeywords() { return keywords; }
    public void setKeywords(String keywords) { this.keywords = keywords; }
    public String getReferenceNotes() { return referenceNotes; }
    public void setReferenceNotes(String referenceNotes) { this.referenceNotes = referenceNotes; }
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
