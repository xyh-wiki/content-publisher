package io.contentpublisher.platform.web.dto;

import io.contentpublisher.platform.domain.ChannelAccountStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateChannelAccountStatusRequest(@Min(1) int expectedVersion,
                                                @NotNull ChannelAccountStatus status) {
}
