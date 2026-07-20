package io.contentpublisher.platform.application.port;

import io.contentpublisher.platform.domain.Article;
import io.contentpublisher.platform.domain.ChannelAccount;
import io.contentpublisher.platform.domain.ChannelType;

import java.util.Map;

public interface ChannelPublisher {
    ChannelType channelType();
    PublishResult publish(ChannelAccount account, PublishContent content, Map<String, String> credentials);

    record PublishContent(Article article, String canonicalUrl) {}
    record PublishResult(String externalId, String externalUrl) {}
}
