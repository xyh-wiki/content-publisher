package io.contentpublisher.platform.infrastructure.channels;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.contentpublisher.platform.application.port.ChannelPublisher;
import io.contentpublisher.platform.domain.ChannelAccount;
import io.contentpublisher.platform.domain.ChannelType;
import io.contentpublisher.platform.infrastructure.config.ChannelProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.util.Map;

@Component
public class XChannelPublisher extends AbstractHttpChannelPublisher {
    public XChannelPublisher(@Qualifier("channelHttpClient") HttpClient httpClient,
                             ObjectMapper objectMapper, ChannelProperties properties) {
        super(httpClient, objectMapper, properties);
    }

    @Override
    public ChannelType channelType() {
        return ChannelType.X;
    }

    @Override
    public PublishResult publish(ChannelAccount account, PublishContent content, Map<String, String> credentials) {
        String text = ChannelContentFormatter.promotionForX(content.article(), content.canonicalUrl());
        HttpRequest request = HttpRequest.newBuilder(URI.create(account.baseUrl() + "/2/tweets"))
                .timeout(properties.timeout()).header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + credentials.get("accessToken"))
                .POST(HttpRequest.BodyPublishers.ofString(json(Map.of("text", text)))).build();
        JsonNode response = send(request);
        String id = required(response.path("data"), "id");
        return new PublishResult(id, "https://x.com/i/web/status/" + id);
    }
}
