package io.contentpublisher.platform.domain;

import java.util.List;

public record AdaptedContent(
        ChannelType channelType,
        ContentFormat format,
        String title,
        String body,
        List<String> tags,
        int characterLimit) {
    public AdaptedContent {
        tags = tags == null ? List.of() : List.copyOf(tags);
    }
}
