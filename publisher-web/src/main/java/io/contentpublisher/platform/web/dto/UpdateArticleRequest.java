package io.contentpublisher.platform.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateArticleRequest(
        @Min(1) int expectedVersion,
        @NotBlank @Size(max = 500) String title,
        @NotBlank @Size(max = 2000) String summary,
        @NotBlank @Size(max = 20000) String markdown,
        @Size(max = 30) List<@Size(max = 100) String> keywords) {
}
