package io.contentpublisher.platform.infrastructure.channels;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.contentpublisher.platform.application.port.ChannelPublisher;
import io.contentpublisher.platform.domain.Article;
import io.contentpublisher.platform.domain.ChannelAccount;
import io.contentpublisher.platform.domain.ChannelType;
import io.contentpublisher.platform.infrastructure.config.ChannelProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class DevChannelPublisher extends AbstractHttpChannelPublisher {
    public DevChannelPublisher(@Qualifier("channelHttpClient") HttpClient httpClient,
                               ObjectMapper objectMapper, ChannelProperties properties) {
        super(httpClient, objectMapper, properties);
    }

    @Override
    public ChannelType channelType() {
        return ChannelType.DEV;
    }

    @Override
    public ChannelPublisher.PublishResult publish(ChannelAccount account, ChannelPublisher.PublishContent content,
                                                  Map<String, String> credentials) {
        Article article = content.article();
        List<String> tags = article.keywords().stream().map(this::tag).filter(value -> !value.isBlank()).distinct()
                .limit(4).toList();
        Map<String, Object> articleBody = new java.util.LinkedHashMap<>();
        articleBody.put("title", article.title()); articleBody.put("published", true);
        articleBody.put("body_markdown", article.markdown()); articleBody.put("tags", tags);
        if (content.canonicalUrl() != null) articleBody.put("canonical_url", content.canonicalUrl());
        Map<String, Object> body = Map.of("article", articleBody);
        HttpRequest request = HttpRequest.newBuilder(URI.create(account.baseUrl() + "/api/articles"))
                .timeout(properties.timeout()).header("Content-Type", "application/json")
                .header("api-key", credentials.get("apiKey"))
                .POST(HttpRequest.BodyPublishers.ofString(json(body))).build();
        JsonNode response = send(request);
        return new ChannelPublisher.PublishResult(required(response, "id"), required(response, "url"));
    }

    private String tag(String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]", "");
    }
}
