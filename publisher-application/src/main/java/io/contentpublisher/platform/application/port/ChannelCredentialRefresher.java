package io.contentpublisher.platform.application.port;

import io.contentpublisher.platform.application.ChannelCredentialRefreshResult;
import io.contentpublisher.platform.domain.ChannelAccount;

import java.util.Map;

public interface ChannelCredentialRefresher {
    ChannelCredentialRefreshResult refresh(ChannelAccount account, Map<String, String> credentials);

    static ChannelCredentialRefresher noop() {
        return (account, credentials) -> ChannelCredentialRefreshResult.unchanged(credentials);
    }
}
