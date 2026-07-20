package io.contentpublisher.platform.application.port;

import io.contentpublisher.platform.domain.ChannelType;

public interface ChannelEndpointPolicy {
    String validateAndNormalize(ChannelType type, String baseUrl);
}
