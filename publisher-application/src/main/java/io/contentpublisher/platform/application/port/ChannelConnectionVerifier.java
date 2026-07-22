package io.contentpublisher.platform.application.port;

import io.contentpublisher.platform.application.ChannelConnectionResult;
import io.contentpublisher.platform.domain.ChannelAccount;

import java.util.Map;

public interface ChannelConnectionVerifier {
    ChannelConnectionResult verify(ChannelAccount account, Map<String, String> credentials);
}
