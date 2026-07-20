package io.contentpublisher.platform.web.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class PasswordChangeRequiredFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof LocalUserPrincipal principal
                && principal.mustChangePassword() && !allowed(request.getRequestURI())) {
            if (request.getRequestURI().startsWith("/api/")) {
                response.setStatus(403);
                response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                response.setContentType("application/json");
                response.getWriter().write("{\"status\":403,\"code\":\"PASSWORD_CHANGE_REQUIRED\","
                        + "\"message\":\"首次登录必须先修改初始密码\"}");
            } else {
                response.sendRedirect("/change-password");
            }
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean allowed(String path) {
        return path.equals("/change-password") || path.equals("/logout") || path.equals("/login")
                || path.startsWith("/assets/") || path.startsWith("/actuator/health")
                || path.equals("/actuator/info");
    }
}
