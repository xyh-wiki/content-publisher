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
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class MediumChannelPublisher extends AbstractHttpChannelPublisher {
    public MediumChannelPublisher(@Qualifier("channelHttpClient") HttpClient httpClient,
                                  ObjectMapper objectMapper, ChannelProperties properties) {
        super(httpClient, objectMapper, properties);
    }

    @Override
    public ChannelType channelType() {
        return ChannelType.MEDIUM;
    }

    @Override
    public PublishResult publish(ChannelAccount account, PublishContent content, Map<String, String> credentials) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("title", content.article().title()); body.put("contentFormat", "markdown");
        body.put("content", content.article().markdown()); body.put("publishStatus", "public");
        body.put("tags", content.article().keywords().stream().limit(5).toList());
        if (content.canonicalUrl() != null) body.put("canonicalUrl", content.canonicalUrl());
        HttpRequest request = HttpRequest.newBuilder(URI.create(account.baseUrl() + "/v1/users/"
                        + credentials.get("authorId") + "/posts"))
                .timeout(properties.timeout()).header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + credentials.get("token"))
                .POST(HttpRequest.BodyPublishers.ofString(json(body))).build();
        JsonNode data = send(request).path("data");
        return new PublishResult(required(data, "id"), required(data, "url"));
    }
}
