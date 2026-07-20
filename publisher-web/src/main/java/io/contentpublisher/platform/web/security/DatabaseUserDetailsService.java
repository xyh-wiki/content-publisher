package io.contentpublisher.platform.web.security;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {
    private final JdbcTemplate jdbcTemplate;

    public DatabaseUserDetailsService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        String normalized = username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
        List<LocalUserRow> users = jdbcTemplate.query("""
                        select id, tenant_id, username, password_hash, enabled, must_change_password
                        from local_users where username = ?
                        """, (resultSet, rowNumber) -> new LocalUserRow(
                        resultSet.getObject("id", UUID.class), resultSet.getString("tenant_id"),
                        resultSet.getString("username"), resultSet.getString("password_hash"),
                        resultSet.getBoolean("enabled"), resultSet.getBoolean("must_change_password")), normalized);
        if (users.isEmpty()) throw new UsernameNotFoundException("用户名或密码错误");
        LocalUserRow user = users.get(0);
        List<SimpleGrantedAuthority> authorities = jdbcTemplate.query(
                "select role from local_user_roles where user_id = ? order by role",
                (resultSet, rowNumber) -> new SimpleGrantedAuthority("ROLE_" + resultSet.getString("role")),
                user.id());
        return new LocalUserPrincipal(user.id(), user.tenantId(), user.username(), user.passwordHash(),
                user.enabled(), user.mustChangePassword(), List.copyOf(authorities));
    }

    private record LocalUserRow(UUID id, String tenantId, String username, String passwordHash,
                                boolean enabled, boolean mustChangePassword) {}
}
