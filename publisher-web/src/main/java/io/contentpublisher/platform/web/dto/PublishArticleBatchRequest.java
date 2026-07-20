package io.contentpublisher.platform.web.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record PublishArticleBatchRequest(
        @NotEmpty @Size(max = 20) List<@NotNull UUID> channelAccountIds,
        @Size(max = 2048) String canonicalUrl) {
}
