package io.contentpublisher.platform.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ImportProjectRequest(
        @NotBlank @Size(max = 2048) String gitUrl,
        @Size(max = 255) String branch) {
}
