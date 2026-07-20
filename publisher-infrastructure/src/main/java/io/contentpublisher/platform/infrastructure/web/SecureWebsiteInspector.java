package io.contentpublisher.platform.infrastructure.web;

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

@Component
public class SecureWebsiteInspector implements WebsiteInspector {
    private final HttpClient httpClient;
    private final WebsiteImportProperties properties;

    public SecureWebsiteInspector(@Qualifier("websiteHttpClient") HttpClient httpClient,
                                  WebsiteImportProperties properties) {
        this.httpClient = httpClient;
        this.properties = properties;
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
            return extractSnapshot(uri, contentType, body);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ApplicationException("WEBSITE_FETCH_INTERRUPTED", "网站抓取被中断", exception);
        } catch (IOException exception) {
            throw new ApplicationException("WEBSITE_FETCH_FAILED", "网站抓取失败", exception);
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
        try (input) {
            byte[] bytes = input.readNBytes(properties.maxResponseBytes() + 1);
            if (bytes.length > properties.maxResponseBytes()) {
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
