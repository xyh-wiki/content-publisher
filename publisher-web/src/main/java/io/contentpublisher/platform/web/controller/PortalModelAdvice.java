package io.contentpublisher.platform.web.controller;

import io.contentpublisher.platform.web.security.LocalUserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

@ControllerAdvice
public class PortalModelAdvice {
    @ModelAttribute("currentUsername")
    public String currentUsername(Authentication authentication) {
        if (authentication == null) return "";
        return authentication.getPrincipal() instanceof LocalUserPrincipal principal
                ? principal.username() : authentication.getName();
    }

    @ModelAttribute("currentTenant")
    public String currentTenant(Authentication authentication) {
        if (authentication == null) return "";
        return authentication.getPrincipal() instanceof LocalUserPrincipal principal ? principal.tenantId() : "-";
    }

    @ModelAttribute("currentRoles")
    public List<String> currentRoles(Authentication authentication) {
        if (authentication == null) return List.of();
        return authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority().replaceFirst("^ROLE_", ""))
                .sorted().toList();
    }

    @ModelAttribute("canAdmin")
    public boolean canAdmin(Authentication authentication) {
        return hasRole(authentication, "ROLE_ADMIN");
    }

    @ModelAttribute("canEdit")
    public boolean canEdit(Authentication authentication) {
        return hasRole(authentication, "ROLE_EDITOR") || hasRole(authentication, "ROLE_ADMIN");
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals(role));
    }
}
