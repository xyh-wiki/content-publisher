package io.contentpublisher.platform.infrastructure.channels;

import io.contentpublisher.platform.application.ApplicationException;
import io.contentpublisher.platform.application.port.ChannelEndpointPolicy;
import io.contentpublisher.platform.domain.ChannelType;
import io.contentpublisher.platform.infrastructure.config.ChannelProperties;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.URI;

@Component
public class SecureChannelEndpointPolicy implements ChannelEndpointPolicy {
    private final ChannelProperties properties;

    public SecureChannelEndpointPolicy(ChannelProperties properties) {
        this.properties = properties;
    }

    @Override
    public String validateAndNormalize(ChannelType type, String baseUrl) {
        String fixed = switch (type) {
            case DEV -> "https://dev.to";
            case GITHUB_DISCUSSIONS -> "https://api.github.com";
            case X -> "https://api.x.com";
            case REDDIT -> "https://oauth.reddit.com";
            case HASHNODE -> "https://gql.hashnode.com";
            case MEDIUM -> "https://api.medium.com";
            case WORDPRESS, DISCOURSE, MASTODON, GHOST -> null;
        };
        if (fixed != null) {
            if (baseUrl != null && !baseUrl.isBlank() && !fixed.equals(stripTrailingSlash(baseUrl.trim()))) {
                throw new ApplicationException("CHANNEL_ENDPOINT_REJECTED", "该渠道不允许覆盖官方 API 地址");
            }
            return fixed;
        }
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new ApplicationException("CHANNEL_ENDPOINT_INVALID", "该渠道必须配置 HTTPS 站点地址");
        }
        try {
            URI uri = URI.create(baseUrl.trim());
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null
                    || uri.getQuery() != null || uri.getFragment() != null) {
                throw new ApplicationException("CHANNEL_ENDPOINT_REJECTED", "渠道地址必须是不含凭据、查询和片段的 HTTPS URL");
            }
            String host = uri.getHost().toLowerCase();
            if (!properties.allowedHosts().contains(host)) {
                throw new ApplicationException("CHANNEL_HOST_REJECTED", "渠道站点不在服务器允许列表中");
            }
            for (InetAddress address : InetAddress.getAllByName(host)) {
                if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                        || address.isSiteLocalAddress() || address.isMulticastAddress()) {
                    throw new ApplicationException("CHANNEL_ADDRESS_REJECTED", "渠道站点解析到了非公网地址");
                }
            }
            return stripTrailingSlash(uri.toString());
        } catch (ApplicationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ApplicationException("CHANNEL_ENDPOINT_INVALID", "渠道站点地址无效", exception);
        }
    }

    private String stripTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
