package io.contentpublisher.platform.infrastructure.ai;

import io.contentpublisher.platform.application.ApplicationException;
import io.contentpublisher.platform.application.port.AiEndpointPolicy;
import io.contentpublisher.platform.infrastructure.config.AiEndpointSecurityProperties;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.URI;
import java.util.Locale;

@Component
public class SecureAiEndpointPolicy implements AiEndpointPolicy {
    private final AiEndpointSecurityProperties properties;

    public SecureAiEndpointPolicy(AiEndpointSecurityProperties properties) {
        this.properties = properties;
    }

    @Override
    public String validateAndNormalize(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank() || baseUrl.length() > 2048) {
            throw new ApplicationException("AI_ENDPOINT_INVALID", "AI Base URL 不能为空且不能超过 2048 个字符");
        }
        try {
            URI uri = URI.create(baseUrl.trim());
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null
                    || uri.getQuery() != null || uri.getFragment() != null) {
                throw new ApplicationException("AI_ENDPOINT_REJECTED",
                        "AI Base URL 必须是不含凭据、查询参数和片段的 HTTPS URL");
            }
            String host = uri.getHost().toLowerCase(Locale.ROOT);
            if (!properties.allowedHosts().isEmpty() && !properties.allowedHosts().contains(host)) {
                throw new ApplicationException("AI_HOST_REJECTED", "AI 服务域名不在服务器允许列表中");
            }
            if (!properties.allowPrivateAddresses()) {
                for (InetAddress address : InetAddress.getAllByName(host)) {
                    if (isNonPublic(address)) {
                        throw new ApplicationException("AI_ADDRESS_REJECTED", "AI 服务解析到了非公网地址");
                    }
                }
            }
            String normalized = uri.toString();
            while (normalized.endsWith("/")) normalized = normalized.substring(0, normalized.length() - 1);
            return normalized;
        } catch (ApplicationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ApplicationException("AI_ENDPOINT_INVALID", "AI Base URL 无效或域名无法解析", exception);
        }
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
