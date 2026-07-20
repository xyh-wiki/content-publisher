package io.contentpublisher.platform.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Set;

@ConfigurationProperties(prefix = "publisher.channels")
public record ChannelProperties(
        boolean enabled,
        String encryptionKey,
        Set<String> allowedHosts,
        Duration timeout) {
    public ChannelProperties {
        allowedHosts = allowedHosts == null ? Set.of() : allowedHosts.stream()
                .map(String::trim).filter(value -> !value.isEmpty()).map(String::toLowerCase).collect(java.util.stream.Collectors.toUnmodifiableSet());
        timeout = timeout == null ? Duration.ofSeconds(30) : timeout;
    }
}
