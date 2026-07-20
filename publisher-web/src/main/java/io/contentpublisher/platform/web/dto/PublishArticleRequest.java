package io.contentpublisher.platform.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record PublishArticleRequest(@NotNull UUID channelAccountId, @Size(max = 2048) String canonicalUrl) {
}
