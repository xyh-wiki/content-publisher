package io.contentpublisher.platform.web.security;

import io.contentpublisher.platform.domain.ActorContext;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class RequestActorProvider {
    private final SecurityProperties properties;
    private final LocalSecurityProperties localProperties;

    public RequestActorProvider(SecurityProperties properties, LocalSecurityProperties localProperties) {
        this.properties = properties;
        this.localProperties = localProperties;
    }

    public ActorContext currentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (localProperties.enabled()) {
            if (authentication == null || !authentication.isAuthenticated()
                    || !(authentication.getPrincipal() instanceof LocalUserPrincipal principal)) {
                throw new AuthenticationCredentialsNotFoundException("请先登录");
            }
            return new ActorContext(principal.tenantId(), principal.username());
        }
        if (!properties.enabled()) {
            return new ActorContext(properties.defaultTenant(), properties.defaultSubject());
        }
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication) || !authentication.isAuthenticated()) {
            throw new AuthenticationCredentialsNotFoundException("缺少有效的访问令牌");
        }
        String tenantId = jwtAuthentication.getToken().getClaimAsString(properties.tenantClaim());
        if (tenantId == null || tenantId.isBlank()) {
            throw new AuthenticationCredentialsNotFoundException("访问令牌缺少租户声明: " + properties.tenantClaim());
        }
        String subject = jwtAuthentication.getToken().getSubject();
        if (subject == null || subject.isBlank()) {
            throw new AuthenticationCredentialsNotFoundException("访问令牌缺少 subject 声明");
        }
        return new ActorContext(tenantId, subject);
    }
}
