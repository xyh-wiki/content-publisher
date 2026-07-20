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
import java.util.Map;

@Component
public class DiscourseChannelPublisher extends AbstractHttpChannelPublisher {
    public DiscourseChannelPublisher(@Qualifier("channelHttpClient") HttpClient httpClient,
                                     ObjectMapper objectMapper, ChannelProperties properties) {
        super(httpClient, objectMapper, properties);
    }

    @Override
    public ChannelType channelType() {
        return ChannelType.DISCOURSE;
    }

    @Override
    public ChannelPublisher.PublishResult publish(ChannelAccount account, ChannelPublisher.PublishContent content,
                                                  Map<String, String> credentials) {
        Article article = content.article();
        String raw = content.canonicalUrl() == null ? article.markdown()
                : article.markdown() + "\n\n原文链接：" + content.canonicalUrl();
        Map<String, Object> body = Map.of("title", article.title(), "raw", raw);
        HttpRequest request = HttpRequest.newBuilder(URI.create(account.baseUrl() + "/posts.json"))
                .timeout(properties.timeout()).header("Content-Type", "application/json")
                .header("Api-Key", credentials.get("apiKey"))
                .header("Api-Username", credentials.get("apiUsername"))
                .POST(HttpRequest.BodyPublishers.ofString(json(body))).build();
        JsonNode response = send(request);
        String topicId = required(response, "topic_id");
        String slug = response.path("topic_slug").asText("topic");
        return new ChannelPublisher.PublishResult(topicId, account.baseUrl() + "/t/" + slug + "/" + topicId);
    }
}
