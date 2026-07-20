package io.contentpublisher.platform.infrastructure.web;

import io.contentpublisher.platform.application.ApplicationException;
import io.contentpublisher.platform.infrastructure.config.WebsiteImportProperties;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;
import java.net.URI;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecureWebsiteInspectorTest {
    private final SecureWebsiteInspector inspector = new SecureWebsiteInspector(
            HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build(),
            new WebsiteImportProperties(Duration.ofSeconds(2), 2_000_000, 100_000, 20));

    @Test
    void shouldRejectHttpAndPrivateWebsiteAddresses() {
        assertThatThrownBy(() -> inspector.inspect("http://example.com"))
                .isInstanceOfSatisfying(ApplicationException.class,
                        exception -> assertThat(exception.code()).isEqualTo("WEBSITE_URL_REJECTED"));
        assertThatThrownBy(() -> inspector.inspect("https://127.0.0.1"))
                .isInstanceOfSatisfying(ApplicationException.class,
                        exception -> assertThat(exception.code()).isEqualTo("WEBSITE_ADDRESS_REJECTED"));
    }

    @Test
    void shouldUseMetadataWhenSinglePageApplicationHasLittleVisibleText() {
        String html = """
                <html><head><title>Example 工具</title>
                <meta property="og:description" content="面向开发者的内容生成与多平台发布工具，支持文章审核和发布管理。">
                </head><body><div id="app"></div><noscript>请启用 JavaScript 查看完整功能。</noscript></body></html>
                """;

        var snapshot = inspector.extractSnapshot(URI.create("https://example.com/product"), "text/html", html);

        assertThat(snapshot.title()).isEqualTo("Example 工具");
        assertThat(snapshot.description()).contains("多平台发布工具");
        assertThat(snapshot.visibleText()).contains("内容生成", "请启用 JavaScript");
    }

    @Test
    void shouldStillRejectPageWithoutAnyUsablePublicInformation() {
        assertThatThrownBy(() -> inspector.extractSnapshot(URI.create("https://example.com"), "text/html",
                "<html><body><div id='app'></div></body></html>"))
                .isInstanceOfSatisfying(ApplicationException.class,
                        exception -> assertThat(exception.code()).isEqualTo("WEBSITE_CONTENT_REJECTED"));
    }
}
