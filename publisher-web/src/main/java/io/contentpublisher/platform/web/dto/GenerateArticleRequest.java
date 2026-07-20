package io.contentpublisher.platform.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record GenerateArticleRequest(
        @NotBlank @Size(max = 20) String language,
        @NotBlank @Size(max = 50) String tone,
        @Min(200) @Max(20_000) int minCharacters,
        @Min(200) @Max(20_000) int maxCharacters,
        @Min(1) @Max(30) int maxKeywords,
        @Size(max = 30) List<@Size(max = 100) String> requiredKeywords,
        @Size(max = 100) List<@Size(max = 100) String> excludedKeywords,
        @Size(max = 20) List<@Size(max = 100) String> requiredSections) {
}
