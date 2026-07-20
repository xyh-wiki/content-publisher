package io.contentpublisher.platform.infrastructure.channels;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
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
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class DevChannelPublisherTest {
    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) server.stop(0);
    }

    @Test
    void shouldPublishApprovedArticleThroughOfficialApi() throws Exception {
        AtomicReference<String> apiKey = new AtomicReference<>();
        AtomicReference<String> requestBody = new AtomicReference<>();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/articles", exchange -> {
            apiKey.set(exchange.getRequestHeaders().getFirst("api-key"));
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = "{\"id\":42,\"url\":\"https://dev.to/example/article\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(201, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
        var publisher = new DevChannelPublisher(HttpClient.newHttpClient(), new ObjectMapper(),
                new ChannelProperties(true, "unused", Set.of(), Duration.ofSeconds(5)));

        var result = publisher.publish(account(baseUrl), new io.contentpublisher.platform.application.port.ChannelPublisher.PublishContent(
                article(), "https://example.com/article"), Map.of("apiKey", "dev-secret"));

        assertThat(result.externalId()).isEqualTo("42");
        assertThat(result.externalUrl()).isEqualTo("https://dev.to/example/article");
        assertThat(apiKey.get()).isEqualTo("dev-secret");
        assertThat(requestBody.get()).contains("企业内容平台", "body_markdown", "published", "canonical_url");
    }

    private ChannelAccount account(String baseUrl) {
        Instant now = Instant.parse("2026-07-20T00:00:00Z");
        return new ChannelAccount(UUID.randomUUID(), "tenant", ChannelType.DEV, "DEV", baseUrl,
                "encrypted", "account-key-001", "a".repeat(64), "b".repeat(64), 1, ChannelAccountStatus.ACTIVE,
                "admin", "admin", now, now);
    }

    private Article article() {
        Instant now = Instant.parse("2026-07-20T00:00:00Z");
        return new Article(UUID.randomUUID(), "tenant", UUID.randomUUID(), UUID.randomUUID(),
                "企业内容平台", "摘要", "## 核心能力\n\n正文", List.of("Java", "内容分发"), "zh-CN",
                "a".repeat(40), 1, ArticleStatus.APPROVED, "editor", "admin", now, now);
    }
}
