package io.contentpublisher.platform.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateWebsiteArticleRequest(
        @NotBlank @Size(max = 2048) @Pattern(regexp = "https://.+") String websiteUrl,
        @NotBlank @Size(max = 2000) String recommendationAngle,
        @NotBlank @Size(max = 500) String audience,
        @Size(max = 30) List<@Size(max = 100) String> keywords,
        @NotBlank @Size(max = 20) String language,
        @NotBlank @Size(max = 50) String tone,
        @Min(200) @Max(3000) int minCharacters,
        @Min(200) @Max(3000) int maxCharacters,
        @Min(1) @Max(30) int maxKeywords,
        @Size(max = 100) List<@Size(max = 100) String> excludedKeywords,
        @Size(max = 20) List<@Size(max = 100) String> requiredSections) {
}
