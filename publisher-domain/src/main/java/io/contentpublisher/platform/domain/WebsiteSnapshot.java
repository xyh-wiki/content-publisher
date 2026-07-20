package io.contentpublisher.platform.domain;

public record WebsiteSnapshot(
        String url,
        String title,
        String description,
        String visibleText) {
}
