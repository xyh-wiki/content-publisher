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
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
public class RedditChannelPublisher extends AbstractHttpChannelPublisher {
    public RedditChannelPublisher(@Qualifier("channelHttpClient") HttpClient httpClient,
                                  ObjectMapper objectMapper, ChannelProperties properties) {
        super(httpClient, objectMapper, properties);
    }

    @Override
    public ChannelType channelType() {
        return ChannelType.REDDIT;
    }

    @Override
    public PublishResult publish(ChannelAccount account, PublishContent content, Map<String, String> credentials) {
        String form = form(Map.of("api_type", "json", "kind", "self",
                "sr", credentials.get("subreddit"),
                "title", ChannelContentFormatter.truncate(content.article().title(), 300),
                "text", ChannelContentFormatter.articleWithLink(content.article(), content.canonicalUrl()),
                "resubmit", "true"));
        HttpRequest request = HttpRequest.newBuilder(URI.create(account.baseUrl() + "/api/submit"))
                .timeout(properties.timeout()).header("Content-Type", "application/x-www-form-urlencoded")
                .header("Authorization", "Bearer " + credentials.get("accessToken"))
                .header("User-Agent", "content-publisher/1.0")
                .POST(HttpRequest.BodyPublishers.ofString(form)).build();
        JsonNode response = send(request);
        JsonNode errors = response.path("json").path("errors");
        if (errors.isArray() && !errors.isEmpty()) {
            throw new io.contentpublisher.platform.application.ApplicationException(
                    "CHANNEL_RESPONSE_REJECTED", "Reddit API 返回业务错误");
        }
        JsonNode data = response.path("json").path("data");
        String url = required(data, "url");
        if (url.startsWith("/")) url = "https://www.reddit.com" + url;
        return new PublishResult(required(data, "name"), url);
    }

    private String form(Map<String, String> values) {
        return values.entrySet().stream().map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
                .collect(java.util.stream.Collectors.joining("&"));
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
