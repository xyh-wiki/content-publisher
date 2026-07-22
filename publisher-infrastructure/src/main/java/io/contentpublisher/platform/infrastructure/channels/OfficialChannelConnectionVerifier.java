package io.contentpublisher.platform.infrastructure.channels;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.contentpublisher.platform.application.ChannelConnectionResult;
import io.contentpublisher.platform.application.port.ChannelConnectionVerifier;
import io.contentpublisher.platform.domain.ChannelAccount;
import io.contentpublisher.platform.infrastructure.config.ChannelProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;

@Component
public class OfficialChannelConnectionVerifier implements ChannelConnectionVerifier {
    private static final String GITHUB_QUERY = """
            query VerifyDiscussionAccess($repositoryId: ID!, $categoryId: ID!) {
              viewer { login }
              repository: node(id: $repositoryId) { ... on Repository { id nameWithOwner } }
              category: node(id: $categoryId) { ... on DiscussionCategory { id name } }
            }
            """;
    private static final String HASHNODE_QUERY = "query VerifyToken { me { id username } }";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final ChannelProperties properties;
    private final Clock clock;

    public OfficialChannelConnectionVerifier(@Qualifier("channelHttpClient") HttpClient httpClient,
                                             ObjectMapper objectMapper, ChannelProperties properties, Clock clock) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public ChannelConnectionResult verify(ChannelAccount account, Map<String, String> credentials) {
        if (!properties.enabled()) return ChannelConnectionResult.failure("渠道发布功能未启用");
        try {
            HttpRequest request = switch (account.type()) {
                case DEV -> get(account.baseUrl() + "/api/users/me").header("api-key", credentials.get("apiKey")).build();
                case WORDPRESS -> get(account.baseUrl() + "/wp-json/wp/v2/users/me?context=edit")
                        .header("Authorization", basic(credentials.get("username"), credentials.get("applicationPassword"))).build();
                case DISCOURSE -> get(account.baseUrl() + "/session/current.json")
                        .header("Api-Key", credentials.get("apiKey"))
                        .header("Api-Username", credentials.get("apiUsername")).build();
                case GITHUB_DISCUSSIONS -> postJson(account.baseUrl() + "/graphql",
                        Map.of("query", GITHUB_QUERY, "variables", Map.of(
                                "repositoryId", credentials.get("repositoryId"),
                                "categoryId", credentials.get("categoryId"))))
                        .header("Authorization", "Bearer " + credentials.get("token"))
                        .header("User-Agent", "content-publisher").build();
                case X -> get(account.baseUrl() + "/2/users/me")
                        .header("Authorization", "Bearer " + credentials.get("accessToken")).build();
                case REDDIT -> get(account.baseUrl() + "/api/v1/me")
                        .header("Authorization", "Bearer " + credentials.get("accessToken"))
                        .header("User-Agent", "content-publisher/1.0").build();
                case HASHNODE -> postJson(account.baseUrl(), Map.of("query", HASHNODE_QUERY))
                        .header("Authorization", credentials.get("token")).build();
                case MEDIUM -> get(account.baseUrl() + "/v1/me")
                        .header("Authorization", "Bearer " + credentials.get("token")).build();
                case MASTODON -> get(account.baseUrl() + "/api/v1/accounts/verify_credentials")
                        .header("Authorization", "Bearer " + credentials.get("accessToken")).build();
                case GHOST -> get(account.baseUrl() + "/ghost/api/admin/site/")
                        .header("Authorization", "Ghost " + ghostToken(credentials.get("adminApiKey"))).build();
                default -> null;
            };
            if (request == null) return ChannelConnectionResult.failure("该平台不支持 API 连接验证");
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return ChannelConnectionResult.failure(statusMessage(response.statusCode()));
            }
            if (account.type() == io.contentpublisher.platform.domain.ChannelType.GITHUB_DISCUSSIONS
                    || account.type() == io.contentpublisher.platform.domain.ChannelType.HASHNODE) {
                JsonNode body = objectMapper.readTree(response.body());
                if (body.has("errors")) return ChannelConnectionResult.failure("GraphQL 凭据或资源校验失败");
                if (account.type() == io.contentpublisher.platform.domain.ChannelType.GITHUB_DISCUSSIONS
                        && (body.path("data").path("repository").isMissingNode()
                        || body.path("data").path("repository").isNull()
                        || body.path("data").path("category").isMissingNode()
                        || body.path("data").path("category").isNull())) {
                    return ChannelConnectionResult.failure("Repository ID 或 Category ID 无效");
                }
            }
            return ChannelConnectionResult.success("连接与凭据验证成功");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return ChannelConnectionResult.failure("连接验证被中断，请重试");
        } catch (Exception exception) {
            return ChannelConnectionResult.failure("无法连接渠道 API，请检查地址、网络和凭据");
        }
    }

    private HttpRequest.Builder get(String url) {
        return HttpRequest.newBuilder(URI.create(url)).timeout(properties.timeout()).GET();
    }

    private HttpRequest.Builder postJson(String url, Object body) throws com.fasterxml.jackson.core.JsonProcessingException {
        return HttpRequest.newBuilder(URI.create(url)).timeout(properties.timeout())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)));
    }

    private String basic(String username, String password) {
        String value = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private String statusMessage(int status) {
        return switch (status) {
            case 401, 403 -> "凭据无效或缺少所需权限（HTTP " + status + "）";
            case 404 -> "接口地址或目标资源不存在（HTTP 404）";
            case 429 -> "平台触发限流，请稍后重试（HTTP 429）";
            default -> "渠道 API 返回 HTTP " + status;
        };
    }

    private String ghostToken(String adminApiKey) throws Exception {
        String[] parts = adminApiKey.split(":", 2);
        if (parts.length != 2 || parts[0].isBlank() || !parts[1].matches("[0-9a-fA-F]{32,}")) {
            throw new IllegalArgumentException("Ghost Admin API Key 格式无效");
        }
        long issuedAt = clock.instant().getEpochSecond();
        String header = base64Url(objectMapper.writeValueAsString(Map.of("alg", "HS256", "typ", "JWT", "kid", parts[0])));
        String payload = base64Url(objectMapper.writeValueAsString(Map.of("iat", issuedAt, "exp", issuedAt + 300,
                "aud", "/admin/")));
        String signingInput = header + "." + payload;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(HexFormat.of().parseHex(parts[1]), "HmacSHA256"));
        return signingInput + "." + Base64.getUrlEncoder().withoutPadding()
                .encodeToString(mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8)));
    }

    private String base64Url(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
