package io.contentpublisher.platform.web.dto;

import io.contentpublisher.platform.domain.ChannelType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;

public record CreateChannelAccountRequest(
        @NotNull ChannelType type,
        @NotBlank @Size(max = 120) String displayName,
        @Size(max = 2048) String baseUrl,
        @NotEmpty @Size(max = 10) Map<@NotBlank @Size(max = 100) String,
                @NotBlank @Size(max = 4000) String> credentials) {
}
