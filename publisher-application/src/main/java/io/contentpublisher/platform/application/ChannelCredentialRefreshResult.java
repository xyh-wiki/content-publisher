package io.contentpublisher.platform.application;

import java.util.Map;

public record ChannelCredentialRefreshResult(Map<String, String> credentials, boolean refreshed) {
    public ChannelCredentialRefreshResult {
        credentials = Map.copyOf(credentials);
    }

    public static ChannelCredentialRefreshResult unchanged(Map<String, String> credentials) {
        return new ChannelCredentialRefreshResult(credentials, false);
    }
}
