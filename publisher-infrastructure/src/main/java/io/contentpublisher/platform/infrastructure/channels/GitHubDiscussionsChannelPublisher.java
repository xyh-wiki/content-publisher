package io.contentpublisher.platform.infrastructure.channels;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.contentpublisher.platform.application.ApplicationException;
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
public class GitHubDiscussionsChannelPublisher extends AbstractHttpChannelPublisher {
    private static final String MUTATION = """
            mutation($input: AddDiscussionInput!) {
              addDiscussion(input: $input) { discussion { id url } }
            }
            """;

    public GitHubDiscussionsChannelPublisher(@Qualifier("channelHttpClient") HttpClient httpClient,
                                             ObjectMapper objectMapper, ChannelProperties properties) {
        super(httpClient, objectMapper, properties);
    }

    @Override
    public ChannelType channelType() {
        return ChannelType.GITHUB_DISCUSSIONS;
    }

    @Override
    public ChannelPublisher.PublishResult publish(ChannelAccount account, ChannelPublisher.PublishContent content,
                                                  Map<String, String> credentials) {
        Article article = content.article();
        String markdown = content.canonicalUrl() == null ? article.markdown()
                : article.markdown() + "\n\n原文链接：" + content.canonicalUrl();
        Map<String, Object> input = Map.of("repositoryId", credentials.get("repositoryId"),
                "categoryId", credentials.get("categoryId"), "title", article.title(), "body", markdown);
        Map<String, Object> body = Map.of("query", MUTATION, "variables", Map.of("input", input));
        HttpRequest request = HttpRequest.newBuilder(URI.create(account.baseUrl() + "/graphql"))
                .timeout(properties.timeout()).header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + credentials.get("token"))
                .header("User-Agent", "content-publisher")
                .POST(HttpRequest.BodyPublishers.ofString(json(body))).build();
        JsonNode response = send(request);
        if (response.has("errors")) {
            throw new ApplicationException("CHANNEL_RESPONSE_REJECTED", "GitHub GraphQL 返回业务错误");
        }
        JsonNode discussion = response.path("data").path("addDiscussion").path("discussion");
        return new ChannelPublisher.PublishResult(required(discussion, "id"), required(discussion, "url"));
    }
}
