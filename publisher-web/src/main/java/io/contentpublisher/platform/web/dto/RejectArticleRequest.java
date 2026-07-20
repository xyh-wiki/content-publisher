package io.contentpublisher.platform.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectArticleRequest(@NotBlank @Size(max = 500) String reason) {
}
