package io.contentpublisher.platform.infrastructure.channels;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.contentpublisher.platform.application.ApplicationException;
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
public class HashnodeChannelPublisher extends AbstractHttpChannelPublisher {
    private static final String MUTATION = """
            mutation PublishPost($input: PublishPostInput!) {
              publishPost(input: $input) { post { id url } }
            }
            """;

    public HashnodeChannelPublisher(@Qualifier("channelHttpClient") HttpClient httpClient,
                                    ObjectMapper objectMapper, ChannelProperties properties) {
        super(httpClient, objectMapper, properties);
    }

    @Override
    public ChannelType channelType() {
        return ChannelType.HASHNODE;
    }

    @Override
    public PublishResult publish(ChannelAccount account, PublishContent content, Map<String, String> credentials) {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("publicationId", credentials.get("publicationId"));
        input.put("title", content.article().title());
        input.put("contentMarkdown", content.article().markdown());
        if (content.canonicalUrl() != null) input.put("originalArticleURL", content.canonicalUrl());
        Map<String, Object> requestBody = Map.of("query", MUTATION, "variables", Map.of("input", input));
        HttpRequest request = HttpRequest.newBuilder(URI.create(account.baseUrl()))
                .timeout(properties.timeout()).header("Content-Type", "application/json")
                .header("Authorization", credentials.get("token"))
                .POST(HttpRequest.BodyPublishers.ofString(json(requestBody))).build();
        JsonNode response = send(request);
        if (response.has("errors")) throw new ApplicationException("CHANNEL_RESPONSE_REJECTED", "Hashnode GraphQL 返回业务错误");
        JsonNode post = response.path("data").path("publishPost").path("post");
        return new PublishResult(required(post, "id"), required(post, "url"));
    }
}
