package io.contentpublisher.platform.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Set;

@ConfigurationProperties("publisher.ai.security")
public record AiEndpointSecurityProperties(Set<String> allowedHosts, boolean allowPrivateAddresses) {
    public AiEndpointSecurityProperties {
        allowedHosts = allowedHosts == null ? Set.of() : allowedHosts.stream()
                .map(String::trim).filter(value -> !value.isBlank()).map(String::toLowerCase)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }
}
