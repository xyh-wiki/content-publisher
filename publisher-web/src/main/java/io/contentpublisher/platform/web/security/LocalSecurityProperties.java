package io.contentpublisher.platform.web.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("publisher.security.local")
public record LocalSecurityProperties(String bootstrapUsername, String bootstrapPassword, String bootstrapTenant,
                                      boolean bootstrapMustChangePassword) {
    public LocalSecurityProperties {
        bootstrapUsername = normalize(bootstrapUsername);
        bootstrapPassword = bootstrapPassword == null ? "" : bootstrapPassword;
        bootstrapTenant = normalize(bootstrapTenant);
        if (bootstrapTenant.isEmpty()) bootstrapTenant = "local";
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
