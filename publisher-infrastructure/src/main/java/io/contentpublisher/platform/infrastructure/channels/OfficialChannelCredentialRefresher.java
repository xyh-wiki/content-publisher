package io.contentpublisher.platform.infrastructure.channels;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.contentpublisher.platform.application.ApplicationException;
import io.contentpublisher.platform.application.ChannelCredentialRefreshResult;
import io.contentpublisher.platform.application.port.ChannelCredentialRefresher;
import io.contentpublisher.platform.domain.ChannelAccount;
import io.contentpublisher.platform.domain.ChannelType;
import io.contentpublisher.platform.infrastructure.config.ChannelProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class OfficialChannelCredentialRefresher implements ChannelCredentialRefresher {
    private static final URI X_TOKEN_URL = URI.create("https://api.x.com/2/oauth2/token");
    private static final URI REDDIT_TOKEN_URL = URI.create("https://www.reddit.com/api/v1/access_token");

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final ChannelProperties properties;
    private final URI xTokenUrl;
    private final URI redditTokenUrl;

    @Autowired
    public OfficialChannelCredentialRefresher(@Qualifier("channelHttpClient") HttpClient httpClient,
                                              ObjectMapper objectMapper, ChannelProperties properties) {
        this(httpClient, objectMapper, properties, X_TOKEN_URL, REDDIT_TOKEN_URL);
    }

    OfficialChannelCredentialRefresher(HttpClient httpClient, ObjectMapper objectMapper,
                                       ChannelProperties properties, URI xTokenUrl, URI redditTokenUrl) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.xTokenUrl = xTokenUrl;
        this.redditTokenUrl = redditTokenUrl;
    }

    @Override
    public ChannelCredentialRefreshResult refresh(ChannelAccount account, Map<String, String> credentials) {
        if (account.type() != ChannelType.X && account.type() != ChannelType.REDDIT) {
            return ChannelCredentialRefreshResult.unchanged(credentials);
        }
        if (!properties.enabled()) {
            throw new ApplicationException("CHANNELS_DISABLED", "渠道发布功能未启用");
        }
        try {
            HttpRequest request = account.type() == ChannelType.X
                    ? xRequest(credentials) : redditRequest(credentials);
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ApplicationException("CHANNEL_AUTH_REFRESH_FAILED",
                        "渠道授权刷新失败（HTTP " + response.statusCode() + "）");
            }
            JsonNode body = objectMapper.readTree(response.body());
            String accessToken = required(body, "access_token");
            String rotatedRefreshToken = body.path("refresh_token").asText("").trim();
            Map<String, String> refreshed = new LinkedHashMap<>(credentials);
            refreshed.put("accessToken", accessToken);
            if (!rotatedRefreshToken.isEmpty()) refreshed.put("refreshToken", rotatedRefreshToken);
            return new ChannelCredentialRefreshResult(refreshed, true);
        } catch (ApplicationException exception) {
            throw exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ApplicationException("CHANNEL_AUTH_REFRESH_INTERRUPTED", "渠道授权刷新被中断，请重试", exception);
        } catch (Exception exception) {
            throw new ApplicationException("CHANNEL_AUTH_REFRESH_FAILED", "无法刷新渠道授权，请检查应用凭据和 Refresh Token", exception);
        }
    }

    private HttpRequest xRequest(Map<String, String> credentials) {
        String body = form(Map.of("grant_type", "refresh_token",
                "refresh_token", credentials.get("refreshToken"),
                "client_id", credentials.get("clientId")));
        return request(xTokenUrl, credentials, body, "content-publisher/1.0");
    }

    private HttpRequest redditRequest(Map<String, String> credentials) {
        String body = form(Map.of("grant_type", "refresh_token",
                "refresh_token", credentials.get("refreshToken")));
        return request(redditTokenUrl, credentials, body, "content-publisher/1.0");
    }

    private HttpRequest request(URI uri, Map<String, String> credentials, String body, String userAgent) {
        String client = credentials.get("clientId") + ":" + credentials.get("clientSecret");
        String authorization = Base64.getEncoder().encodeToString(client.getBytes(StandardCharsets.UTF_8));
        return HttpRequest.newBuilder(uri).timeout(properties.timeout())
                .header("Authorization", "Basic " + authorization)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("User-Agent", userAgent)
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();
    }

    private String form(Map<String, String> values) {
        return values.entrySet().stream()
                .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
                .collect(Collectors.joining("&"));
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String required(JsonNode body, String field) {
        String value = body.path(field).asText("").trim();
        if (value.isEmpty()) {
            throw new ApplicationException("CHANNEL_AUTH_REFRESH_FAILED", "渠道授权响应缺少访问令牌");
        }
        return value;
    }
}
