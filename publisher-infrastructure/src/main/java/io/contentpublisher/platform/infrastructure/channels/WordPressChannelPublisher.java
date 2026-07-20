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
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Component
public class WordPressChannelPublisher extends AbstractHttpChannelPublisher {
    private final Parser markdownParser = Parser.builder().build();
    private final HtmlRenderer htmlRenderer = HtmlRenderer.builder().escapeHtml(true).build();

    public WordPressChannelPublisher(@Qualifier("channelHttpClient") HttpClient httpClient,
                                     ObjectMapper objectMapper, ChannelProperties properties) {
        super(httpClient, objectMapper, properties);
    }

    @Override
    public ChannelType channelType() {
        return ChannelType.WORDPRESS;
    }

    @Override
    public ChannelPublisher.PublishResult publish(ChannelAccount account, ChannelPublisher.PublishContent content,
                                                  Map<String, String> credentials) {
        Article article = content.article();
        String basic = Base64.getEncoder().encodeToString((credentials.get("username") + ":"
                + credentials.get("applicationPassword")).getBytes(StandardCharsets.UTF_8));
        String markdown = ChannelContentFormatter.articleWithLink(article, content.canonicalUrl());
        String html = htmlRenderer.render(markdownParser.parse(markdown));
        Map<String, Object> body = Map.of("title", article.title(), "content", html, "status", "publish");
        HttpRequest request = HttpRequest.newBuilder(URI.create(account.baseUrl() + "/wp-json/wp/v2/posts"))
                .timeout(properties.timeout()).header("Content-Type", "application/json")
                .header("Authorization", "Basic " + basic)
                .POST(HttpRequest.BodyPublishers.ofString(json(body))).build();
        JsonNode response = send(request);
        return new ChannelPublisher.PublishResult(required(response, "id"), required(response, "link"));
    }
}
