package io.contentpublisher.platform.infrastructure.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.contentpublisher.platform.application.ApplicationException;
import io.contentpublisher.platform.application.port.WebsiteInspector;
import io.contentpublisher.platform.domain.WebsiteSnapshot;
import io.contentpublisher.platform.infrastructure.config.WebsiteImportProperties;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class SecureWebsiteInspector implements WebsiteInspector {
    private static final Pattern PUBLIC_API_PATH = Pattern.compile("/api/[A-Za-z0-9_./-]{1,180}");
    private static final Set<String> UNSAFE_API_MARKERS = Set.of(
            "/admin", "/auth", "/login", "/logout", "/token", "/session", "/delete", "/create",
            "/update", "/batch", "/fetch", "/trigger", "/refresh");
    private static final Set<String> PUBLIC_CONTENT_MARKERS = Set.of(
            "trending", "language", "tag", "repository", "search", "public", "content", "article",
            "post", "document", "catalog", "feed");
    private final HttpClient httpClient;
    private final WebsiteImportProperties properties;
    private final ObjectMapper objectMapper;

    public SecureWebsiteInspector(@Qualifier("websiteHttpClient") HttpClient httpClient,
                                  WebsiteImportProperties properties, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public WebsiteSnapshot inspect(String websiteUrl) {
        URI uri = validate(websiteUrl);
        HttpRequest request = HttpRequest.newBuilder(uri).timeout(properties.timeout())
                .header("Accept", "text/html, text/plain;q=0.8")
                .header("User-Agent", "ContentPublisher/1.0 WebsiteInspector")
                .GET().build();
        try {
            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                response.body().close();
                throw new ApplicationException("WEBSITE_FETCH_FAILED", "网站返回异常状态: " + response.statusCode());
            }
            String contentType = response.headers().firstValue("Content-Type").orElse("")
                    .toLowerCase(Locale.ROOT);
            if (!contentType.startsWith("text/html") && !contentType.startsWith("text/plain")) {
                response.body().close();
                throw new ApplicationException("WEBSITE_CONTENT_REJECTED", "网站响应不是 HTML 或纯文本");
            }
            String body = readLimited(response.body());
            try {
                return extractSnapshot(uri, contentType, body);
            } catch (ApplicationException exception) {
                if (!"WEBSITE_CONTENT_REJECTED".equals(exception.code()) || !contentType.startsWith("text/html")) {
                    throw exception;
                }
                return extractSpaSnapshot(uri, body);
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ApplicationException("WEBSITE_FETCH_INTERRUPTED", "网站抓取被中断", exception);
        } catch (IOException exception) {
            throw new ApplicationException("WEBSITE_FETCH_FAILED", "网站抓取失败", exception);
        }
    }

    private WebsiteSnapshot extractSpaSnapshot(URI uri, String html) throws IOException, InterruptedException {
        Document document = Jsoup.parse(html, uri.toString());
        List<URI> scriptUris = document.select("script[src]").stream()
                .map(element -> sameOrigin(uri, element.absUrl("src")))
                .filter(java.util.Objects::nonNull)
                .filter(script -> script.getPath() != null && script.getPath().endsWith(".js"))
                .limit(3)
                .toList();
        LinkedHashSet<String> apiPaths = new LinkedHashSet<>();
        for (URI scriptUri : scriptUris) {
            String script = fetchText(scriptUri, "application/javascript,text/javascript;q=0.9,*/*;q=0.1",
                    Set.of("application/javascript", "text/javascript", "application/x-javascript"),
                    properties.maxResponseBytes());
            if (script != null) apiPaths.addAll(discoverPublicApiPaths(script));
            if (apiPaths.size() >= 8) break;
        }
        LinkedHashSet<String> publicText = new LinkedHashSet<>();
        int fetchedEndpoints = 0;
        for (String path : apiPaths) {
            URI endpoint = uri.resolve(path);
            String json = fetchText(endpoint, "application/json", Set.of("application/json", "text/json"),
                    Math.min(properties.maxResponseBytes(), 500_000));
            if (json == null) continue;
            addText(publicText, extractJsonText(json));
            fetchedEndpoints += 1;
            if (fetchedEndpoints >= 3 || String.join("。", publicText).length() >= properties.maxTextCharacters()) break;
        }
        String text = limitText(String.join("。", publicText));
        requireUsableText(text);
        String title = document.title().isBlank() ? uri.getHost() : document.title().trim();
        String description = firstContent(document, "meta[name=description]", "meta[property=og:description]",
                "meta[name=twitter:description]");
        if (description.isBlank()) description = "从网站同源公开只读接口提取的动态页面信息";
        return new WebsiteSnapshot(uri.toString(), limited(title, 300), limited(description, 2000), text);
    }

    List<String> discoverPublicApiPaths(String script) {
        LinkedHashSet<String> discovered = new LinkedHashSet<>();
        Matcher matcher = PUBLIC_API_PATH.matcher(script == null ? "" : script);
        while (matcher.find()) {
            String path = matcher.group();
            String lower = path.toLowerCase(Locale.ROOT);
            if (path.endsWith("/") || path.contains("..")
                    || UNSAFE_API_MARKERS.stream().anyMatch(lower::contains)
                    || PUBLIC_CONTENT_MARKERS.stream().noneMatch(lower::contains)) continue;
            discovered.add(path);
            if (discovered.size() >= 30) break;
        }
        return discovered.stream().sorted(Comparator
                .comparingInt((String path) -> path.split("/").length)
                .thenComparingInt(String::length)).toList();
    }

    String extractJsonText(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            List<String> values = new ArrayList<>();
            collectJsonText(root, values);
            return limitText(String.join("。", values));
        } catch (Exception exception) {
            return "";
        }
    }

    private void collectJsonText(JsonNode node, List<String> values) {
        if (node == null || values.size() >= 1_000) return;
        if (node.isTextual()) {
            String value = node.asText().replaceAll("\\s+", " ").trim();
            if (value.length() >= 2 && value.length() <= 2_000) values.add(value);
            return;
        }
        if (node.isArray()) {
            node.elements().forEachRemaining(child -> collectJsonText(child, values));
            return;
        }
        if (node.isObject()) node.elements().forEachRemaining(child -> collectJsonText(child, values));
    }

    private String fetchText(URI uri, String accept, Set<String> acceptedContentTypes, int maxBytes)
            throws IOException, InterruptedException {
        ensureSameOriginPublic(uri);
        HttpRequest request = HttpRequest.newBuilder(uri).timeout(properties.timeout())
                .header("Accept", accept)
                .header("User-Agent", "ContentPublisher/1.0 WebsiteInspector")
                .GET().build();
        HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            response.body().close();
            return null;
        }
        String contentType = response.headers().firstValue("Content-Type").orElse("")
                .toLowerCase(Locale.ROOT).split(";", 2)[0].trim();
        if (acceptedContentTypes.stream().noneMatch(contentType::equals)) {
            response.body().close();
            return null;
        }
        try {
            return readLimited(response.body(), maxBytes);
        } catch (ApplicationException exception) {
            if ("WEBSITE_RESPONSE_TOO_LARGE".equals(exception.code())) return null;
            throw exception;
        }
    }

    private URI sameOrigin(URI origin, String candidate) {
        if (candidate == null || candidate.isBlank()) return null;
        try {
            URI resolved = origin.resolve(candidate);
            return sameOrigin(origin, resolved) ? resolved : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean sameOrigin(URI first, URI second) {
        return "https".equalsIgnoreCase(second.getScheme())
                && first.getHost().equalsIgnoreCase(second.getHost())
                && effectivePort(first) == effectivePort(second)
                && second.getUserInfo() == null && second.getFragment() == null;
    }

    private int effectivePort(URI uri) {
        return uri.getPort() == -1 ? 443 : uri.getPort();
    }

    private void ensureSameOriginPublic(URI uri) throws IOException {
        for (InetAddress address : InetAddress.getAllByName(uri.getHost())) {
            if (isNonPublic(address)) throw new IOException("动态资源地址解析到非公网 IP");
        }
    }

    WebsiteSnapshot extractSnapshot(URI uri, String contentType, String body) {
        if (contentType.startsWith("text/plain")) {
            String text = limitText(body);
            requireUsableText(text);
            return new WebsiteSnapshot(uri.toString(), uri.getHost(), "", text);
        }
        Document document = Jsoup.parse(body, uri.toString());
        String documentTitle = document.title().trim();
        String description = firstContent(document, "meta[name=description]", "meta[property=og:description]",
                "meta[name=twitter:description]");
        String socialTitle = firstContent(document, "meta[property=og:title]", "meta[name=twitter:title]");
        document.select("script,style,svg,canvas,iframe").remove();
        String visibleText = document.body() == null ? document.text() : document.body().text();
        java.util.LinkedHashSet<String> sources = new java.util.LinkedHashSet<>();
        addText(sources, visibleText);
        addText(sources, description);
        addText(sources, documentTitle);
        addText(sources, socialTitle);
        String text = limitText(String.join("。", sources));
        requireUsableText(text);
        String title = documentTitle.isBlank() ? (socialTitle.isBlank() ? uri.getHost() : socialTitle) : documentTitle;
        return new WebsiteSnapshot(uri.toString(), limited(title, 300), limited(description, 2000), text);
    }

    private String firstContent(Document document, String... selectors) {
        for (String selector : selectors) {
            String value = document.select(selector).attr("content").trim();
            if (!value.isBlank()) return value;
        }
        return "";
    }

    private void addText(java.util.Set<String> values, String value) {
        String normalized = value == null ? "" : value.replaceAll("\\s+", " ").trim();
        if (!normalized.isBlank()) values.add(normalized);
    }

    private void requireUsableText(String text) {
        if (text.length() < properties.minTextCharacters()) {
            throw new ApplicationException("WEBSITE_CONTENT_REJECTED",
                    "网站未提供足够的公开正文或描述信息，可能需要登录或依赖浏览器脚本加载");
        }
    }

    private URI validate(String value) {
        if (value == null || value.isBlank() || value.length() > 2048) {
            throw new ApplicationException("WEBSITE_URL_INVALID", "网站链接不能为空且不能超过 2048 个字符");
        }
        try {
            URI uri = URI.create(value == null ? "" : value.trim());
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null
                    || uri.getFragment() != null) {
                throw new ApplicationException("WEBSITE_URL_REJECTED",
                        "网站链接必须为不含账号密码和片段的公网 HTTPS URL");
            }
            for (InetAddress address : InetAddress.getAllByName(uri.getHost())) {
                if (isNonPublic(address)) {
                    throw new ApplicationException("WEBSITE_ADDRESS_REJECTED", "网站地址解析到非公网 IP");
                }
            }
            return uri.normalize();
        } catch (ApplicationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ApplicationException("WEBSITE_URL_INVALID", "网站链接格式无效", exception);
        }
    }

    private String readLimited(InputStream input) throws IOException {
        return readLimited(input, properties.maxResponseBytes());
    }

    private String readLimited(InputStream input, int maxBytes) throws IOException {
        try (input) {
            byte[] bytes = input.readNBytes(maxBytes + 1);
            if (bytes.length > maxBytes) {
                throw new ApplicationException("WEBSITE_RESPONSE_TOO_LARGE", "网站响应超过大小限制");
            }
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }

    private String limitText(String value) {
        String normalized = value.replaceAll("\\s+", " ").trim();
        return limited(normalized, properties.maxTextCharacters());
    }

    private String limited(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }

    private boolean isNonPublic(InetAddress address) {
        if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                || address.isSiteLocalAddress() || address.isMulticastAddress()) return true;
        byte[] bytes = address.getAddress();
        if (bytes.length == 4) {
            int first = Byte.toUnsignedInt(bytes[0]);
            int second = Byte.toUnsignedInt(bytes[1]);
            return first == 0 || first >= 224 || (first == 100 && second >= 64 && second <= 127)
                    || (first == 198 && (second == 18 || second == 19));
        }
        return (bytes[0] & 0xfe) == 0xfc;
    }
}
