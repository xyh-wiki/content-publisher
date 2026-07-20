package io.contentpublisher.platform.infrastructure.channels;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.contentpublisher.platform.application.ApplicationException;
import io.contentpublisher.platform.application.port.ChannelPublisher;
import io.contentpublisher.platform.domain.ChannelAccount;
import io.contentpublisher.platform.domain.ChannelType;
import io.contentpublisher.platform.infrastructure.config.ChannelProperties;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class GhostChannelPublisher extends AbstractHttpChannelPublisher {
    private final Clock clock;
    private final Parser markdownParser = Parser.builder().build();
    private final HtmlRenderer htmlRenderer = HtmlRenderer.builder().escapeHtml(true).build();

    public GhostChannelPublisher(@Qualifier("channelHttpClient") HttpClient httpClient,
                                 ObjectMapper objectMapper, ChannelProperties properties, Clock clock) {
        super(httpClient, objectMapper, properties);
        this.clock = clock;
    }

    @Override
    public ChannelType channelType() {
        return ChannelType.GHOST;
    }

    @Override
    public PublishResult publish(ChannelAccount account, PublishContent content, Map<String, String> credentials) {
        Map<String, Object> post = new LinkedHashMap<>();
        post.put("title", content.article().title());
        post.put("html", htmlRenderer.render(markdownParser.parse(content.article().markdown())));
        post.put("status", "published");
        if (content.canonicalUrl() != null) post.put("canonical_url", content.canonicalUrl());
        HttpRequest request = HttpRequest.newBuilder(URI.create(account.baseUrl()
                        + "/ghost/api/admin/posts/?source=html"))
                .timeout(properties.timeout()).header("Content-Type", "application/json")
                .header("Authorization", "Ghost " + token(credentials.get("adminApiKey")))
                .POST(HttpRequest.BodyPublishers.ofString(json(Map.of("posts", List.of(post))))).build();
        JsonNode response = send(request).path("posts");
        if (!response.isArray() || response.isEmpty()) {
            throw new ApplicationException("CHANNEL_RESPONSE_INVALID", "Ghost API 响应缺少文章记录");
        }
        JsonNode created = response.get(0);
        return new PublishResult(required(created, "id"), required(created, "url"));
    }

    private String token(String adminApiKey) {
        try {
            String[] parts = adminApiKey.split(":", 2);
            if (parts.length != 2 || parts[0].isBlank() || !parts[1].matches("[0-9a-fA-F]{32,}")) {
                throw new ApplicationException("CHANNEL_CREDENTIALS_INVALID", "Ghost Admin API Key 格式无效");
            }
            long issuedAt = clock.instant().getEpochSecond();
            String header = base64Url(json(Map.of("alg", "HS256", "typ", "JWT", "kid", parts[0])));
            String payload = base64Url(json(Map.of("iat", issuedAt, "exp", issuedAt + 300, "aud", "/admin/")));
            String signingInput = header + "." + payload;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(java.util.HexFormat.of().parseHex(parts[1]), "HmacSHA256"));
            return signingInput + "." + Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8)));
        } catch (ApplicationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ApplicationException("CHANNEL_CREDENTIALS_INVALID", "Ghost Admin API Key 无法签名", exception);
        }
    }

    private String base64Url(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
