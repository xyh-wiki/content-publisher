package io.contentpublisher.platform.infrastructure.ai;

import io.contentpublisher.platform.application.ApplicationException;
import io.contentpublisher.platform.domain.GenerationPolicy;
import io.contentpublisher.platform.domain.RepositorySnapshot;
import io.contentpublisher.platform.infrastructure.config.AiProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpenAiCompatibleContentGeneratorTest {
    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) server.stop(0);
    }

    @Test
    void shouldNormalizeKeywordsAndAcceptControlledOutput() throws Exception {
        String markdown = "## 项目概览\nGit 自动分析平台。" + "这是经过仓库事实校验的技术说明。".repeat(20);
        OpenAiCompatibleContentGenerator generator = generator(aiEnvelope(markdown, List.of("Java", "AI", "多余词")));
        GenerationPolicy policy = new GenerationPolicy("zh-CN", "专业", 200, 2000, 2,
                List.of("Git"), List.of("夸大宣传"), List.of("项目概览"));

        var result = generator.generate(snapshot(), policy);

        assertThat(result.keywords()).containsExactly("Git", "Java");
        assertThat(result.markdown()).contains("## 项目概览", "Git");
    }

    @Test
    void shouldRejectForbiddenKeywordInGeneratedArticle() throws Exception {
        String markdown = "## 项目概览\nGit 夸大宣传。" + "技术说明。".repeat(50);
        OpenAiCompatibleContentGenerator generator = generator(aiEnvelope(markdown, List.of("Git")));
        GenerationPolicy policy = new GenerationPolicy("zh-CN", "专业", 200, 2000, 5,
                List.of("Git"), List.of("夸大宣传"), List.of("项目概览"));

        assertThatThrownBy(() -> generator.generate(snapshot(), policy))
                .isInstanceOf(ApplicationException.class)
                .extracting(exception -> ((ApplicationException) exception).code())
                .isEqualTo("AI_OUTPUT_REJECTED");
    }

    private OpenAiCompatibleContentGenerator generator(String response) throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            exchange.getRequestBody().readAllBytes();
            byte[] body = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        URI baseUrl = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/v1");
        AiProperties properties = new AiProperties(true, baseUrl, "test-key", "test-model", Duration.ofSeconds(5), 0.2);
        return new OpenAiCompatibleContentGenerator(properties, HttpClient.newHttpClient(), new ObjectMapper());
    }

    private String aiEnvelope(String markdown, List<String> keywords) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String content = mapper.writeValueAsString(java.util.Map.of(
                "title", "Git 项目分析平台",
                "summary", "依据仓库事实生成文章。",
                "markdown", markdown,
                "keywords", keywords));
        return mapper.writeValueAsString(java.util.Map.of("choices", List.of(
                java.util.Map.of("message", java.util.Map.of("content", content)))));
    }

    private RepositorySnapshot snapshot() {
        return new RepositorySnapshot("publisher", "内容发布平台", "main", "abc123", "README",
                "pom.xml", List.of("pom.xml", "src/Main.java"), List.of("Java"), "LICENSE");
    }
}
