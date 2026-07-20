package io.contentpublisher.platform.web.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;
import java.util.UUID;

public record LocalUserPrincipal(UUID id, String tenantId, String username, String password,
                                 boolean enabled, boolean mustChangePassword,
                                 List<GrantedAuthority> grantedAuthorities) implements UserDetails {
    public LocalUserPrincipal {
        grantedAuthorities = List.copyOf(grantedAuthorities);
    }

    @Override
    public List<GrantedAuthority> getAuthorities() {
        return grantedAuthorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
