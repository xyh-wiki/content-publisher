package io.contentpublisher.platform.web.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("publisher.security")
public record SecurityProperties(boolean enabled, String tenantClaim, String rolesClaim,
                                 String defaultTenant, String defaultSubject) {
    public SecurityProperties {
        tenantClaim = blankToDefault(tenantClaim, "tenant_id");
        rolesClaim = blankToDefault(rolesClaim, "roles");
        defaultTenant = blankToDefault(defaultTenant, "local");
        defaultSubject = blankToDefault(defaultSubject, "local-developer");
    }

    private static String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }
}
