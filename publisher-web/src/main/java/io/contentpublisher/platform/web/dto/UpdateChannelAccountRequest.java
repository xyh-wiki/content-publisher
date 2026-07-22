package io.contentpublisher.platform.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateChannelAccountRequest(
        @Min(1) int expectedVersion,
        @NotBlank @Size(max = 120) String displayName,
        @Size(max = 2048) String baseUrl) {
}
