package io.contentpublisher.platform.infrastructure.web;

import io.contentpublisher.platform.application.ApplicationException;
import io.contentpublisher.platform.infrastructure.config.WebsiteImportProperties;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecureWebsiteInspectorTest {
    private final SecureWebsiteInspector inspector = new SecureWebsiteInspector(
            HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build(),
            new WebsiteImportProperties(Duration.ofSeconds(2), 2_000_000, 100_000));

    @Test
    void shouldRejectHttpAndPrivateWebsiteAddresses() {
        assertThatThrownBy(() -> inspector.inspect("http://example.com"))
                .isInstanceOfSatisfying(ApplicationException.class,
                        exception -> assertThat(exception.code()).isEqualTo("WEBSITE_URL_REJECTED"));
        assertThatThrownBy(() -> inspector.inspect("https://127.0.0.1"))
                .isInstanceOfSatisfying(ApplicationException.class,
                        exception -> assertThat(exception.code()).isEqualTo("WEBSITE_ADDRESS_REJECTED"));
    }
}
