package io.contentpublisher.platform.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MarkdownPreviewRequest(@NotNull @Size(max = 20000) String markdown) {
}
