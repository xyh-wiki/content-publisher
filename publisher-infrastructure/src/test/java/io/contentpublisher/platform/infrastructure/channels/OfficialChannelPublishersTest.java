package io.contentpublisher.platform.infrastructure.channels;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import io.contentpublisher.platform.application.ApplicationException;
import io.contentpublisher.platform.application.PlatformContentAdapter;
import io.contentpublisher.platform.application.port.ChannelPublisher;
import io.contentpublisher.platform.domain.Article;
import io.contentpublisher.platform.domain.ArticleStatus;
import io.contentpublisher.platform.domain.ChannelAccount;
import io.contentpublisher.platform.domain.ChannelAccountStatus;
import io.contentpublisher.platform.domain.ChannelType;
import io.contentpublisher.platform.infrastructure.config.ChannelProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OfficialChannelPublishersTest {
    private HttpServer server;

    @AfterEach
    void stop() {
        if (server != null) server.stop(0);
    }

    @Test
    void shouldPublishShortPostToX() throws Exception {
        AtomicReference<String> body = new AtomicReference<>();
        start("/2/tweets", body, "{\"data\":{\"id\":\"12345\"}}");
        var publisher = new XChannelPublisher(HttpClient.newHttpClient(), new ObjectMapper(), properties());

        var result = publisher.publish(account(ChannelType.X), content(ChannelType.X), Map.of("accessToken", "x-token"));

        assertThat(result.externalUrl()).isEqualTo("https://x.com/i/web/status/12345");
        assertThat(body.get()).contains("https://example.com/article");
    }

    @Test
    void shouldKeepXPostWithinUnicodeSafeLimit() throws Exception {
        AtomicReference<String> body = new AtomicReference<>();
        start("/2/tweets", body, "{\"data\":{\"id\":\"12345\"}}");
        var publisher = new XChannelPublisher(HttpClient.newHttpClient(), new ObjectMapper(), properties());
        Article article = article("技术".repeat(200), "能力".repeat(300));

        String canonicalUrl = "https://example.com/article";
        publisher.publish(account(ChannelType.X), new ChannelPublisher.PublishContent(article,
                new PlatformContentAdapter().adapt(article, ChannelType.X, canonicalUrl), canonicalUrl),
                Map.of("accessToken", "x-token"));

        String text = new ObjectMapper().readTree(body.get()).path("text").asText();
        assertThat(text.codePointCount(0, text.length())).isLessThanOrEqualTo(280);
    }

    @Test
    void shouldAppendCanonicalLinkToWordPressContent() throws Exception {
        AtomicReference<String> body = new AtomicReference<>();
        start("/wp-json/wp/v2/posts", body, "{\"id\":42,\"link\":\"https://blog.example/post\"}");
        var publisher = new WordPressChannelPublisher(HttpClient.newHttpClient(), new ObjectMapper(), properties());

        publisher.publish(account(ChannelType.WORDPRESS), content(ChannelType.WORDPRESS),
                Map.of("username", "publisher", "applicationPassword", "app-password"));

        assertThat(body.get()).contains("https://example.com/article").doesNotContain("<script>");
    }

    @Test
    void shouldRejectHashnodeGraphqlBusinessErrors() throws Exception {
        AtomicReference<String> body = new AtomicReference<>();
        start("/", body, "{\"errors\":[{\"message\":\"rejected\"}]}");
        var publisher = new HashnodeChannelPublisher(HttpClient.newHttpClient(), new ObjectMapper(), properties());

        assertThatThrownBy(() -> publisher.publish(account(ChannelType.HASHNODE), content(ChannelType.HASHNODE),
                Map.of("token", "hashnode-token", "publicationId", "publication-id")))
                .isInstanceOfSatisfying(ApplicationException.class,
                        exception -> assertThat(exception.code()).isEqualTo("CHANNEL_RESPONSE_REJECTED"));
    }

    @Test
    void shouldRejectRedditBusinessErrors() throws Exception {
        AtomicReference<String> body = new AtomicReference<>();
        start("/api/submit", body, "{\"json\":{\"errors\":[[\"BAD_SR_NAME\",\"invalid\",\"sr\"]]}}");
        var publisher = new RedditChannelPublisher(HttpClient.newHttpClient(), new ObjectMapper(), properties());

        assertThatThrownBy(() -> publisher.publish(account(ChannelType.REDDIT), content(ChannelType.REDDIT),
                Map.of("accessToken", "reddit-token", "subreddit", "java")))
                .isInstanceOfSatisfying(ApplicationException.class,
                        exception -> assertThat(exception.code()).isEqualTo("CHANNEL_RESPONSE_REJECTED"));
    }

    @Test
    void shouldSignGhostAdminRequestAndEscapeRawHtml() throws Exception {
        AtomicReference<String> body = new AtomicReference<>();
        AtomicReference<String> authorization = new AtomicReference<>();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/ghost/api/admin/posts/", exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = "{\"posts\":[{\"id\":\"ghost-1\",\"url\":\"https://blog.example/post\"}]}"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(201, response.length);
            exchange.getResponseBody().write(response); exchange.close();
        });
        server.start();
        var publisher = new GhostChannelPublisher(HttpClient.newHttpClient(), new ObjectMapper(), properties(),
                Clock.fixed(Instant.parse("2026-07-20T00:00:00Z"), ZoneOffset.UTC));

        var result = publisher.publish(account(ChannelType.GHOST), content(ChannelType.GHOST),
                Map.of("adminApiKey", "key-id:00112233445566778899aabbccddeeff"));

        assertThat(result.externalId()).isEqualTo("ghost-1");
        assertThat(authorization.get()).startsWith("Ghost ");
        assertThat(authorization.get().substring(6).split("\\.")).hasSize(3);
        assertThat(body.get()).contains("canonical_url").doesNotContain("<script>");
    }

    private void start(String path, AtomicReference<String> body, String json) throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(path, exchange -> {
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = json.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(201, response.length);
            exchange.getResponseBody().write(response); exchange.close();
        });
        server.start();
    }

    private ChannelAccount account(ChannelType type) {
        Instant now = Instant.parse("2026-07-20T00:00:00Z");
        return new ChannelAccount(UUID.randomUUID(), "tenant", type, type.name(),
                "http://127.0.0.1:" + server.getAddress().getPort(), "encrypted", "channel-key-001",
                "a".repeat(64), "b".repeat(64), 1, ChannelAccountStatus.ACTIVE,
                "admin", "admin", now, now);
    }

    private ChannelPublisher.PublishContent content(ChannelType channelType) {
        Instant now = Instant.parse("2026-07-20T00:00:00Z");
        String title = "Enterprise Content Platform";
        String summary = "Technical summary";
        String markdown = "# Body\n\n<script>alert(1)</script>";
        Article article = new Article(UUID.randomUUID(), "tenant", io.contentpublisher.platform.domain.ContentOrigin.git(UUID.randomUUID()),
                UUID.randomUUID(), title, summary, markdown, List.of("Java"), List.of("Java"),
                title, summary, markdown, List.of("Java"), List.of("Java"),
                "en", "a".repeat(40), 1, ArticleStatus.APPROVED, "editor", "admin", now, now);
        String canonicalUrl = "https://example.com/article";
        return new ChannelPublisher.PublishContent(article,
                new PlatformContentAdapter().adapt(article, channelType, canonicalUrl), canonicalUrl);
    }

    private Article article(String title, String summary) {
        Instant now = Instant.parse("2026-07-20T00:00:00Z");
        return new Article(UUID.randomUUID(), "tenant", io.contentpublisher.platform.domain.ContentOrigin.git(UUID.randomUUID()),
                UUID.randomUUID(), title, summary, "# Body", List.of("Java"), List.of("Java"),
                title, summary, "# Body", List.of("Java"), List.of("Java"),
                "zh-CN", "a".repeat(40), 1, ArticleStatus.APPROVED, "editor", "admin", now, now);
    }

    private ChannelProperties properties() {
        return new ChannelProperties(true, "unused", Set.of(), Duration.ofSeconds(5));
    }
}
