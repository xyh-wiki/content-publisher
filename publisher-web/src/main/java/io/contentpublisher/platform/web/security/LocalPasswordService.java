package io.contentpublisher.platform.web.security;

import io.contentpublisher.platform.application.ApplicationException;
import io.contentpublisher.platform.application.port.AuditRecorder;
import io.contentpublisher.platform.domain.ActorContext;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Clock;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@ConditionalOnProperty(name = "publisher.security.local.enabled", havingValue = "true")
public class LocalPasswordService {
    private static final Set<String> DISALLOWED = Set.of(
            "admin123", "password", "password123", "12345678", "qwerty123", "admin123456");

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final AuditRecorder auditRecorder;
    private final Clock clock;

    public LocalPasswordService(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder,
                                AuditRecorder auditRecorder, Clock clock) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
        this.auditRecorder = auditRecorder;
        this.clock = clock;
    }

    @Transactional
    public void changePassword(LocalUserPrincipal principal, String currentPassword,
                               String newPassword, String confirmation) {
        if (currentPassword == null || !passwordEncoder.matches(currentPassword, principal.password())) {
            throw new ApplicationException("CURRENT_PASSWORD_INVALID", "当前密码不正确");
        }
        validateNewPassword(principal.username(), newPassword, confirmation);
        if (passwordEncoder.matches(newPassword, principal.password())) {
            throw new ApplicationException("PASSWORD_REUSE_REJECTED", "新密码不能与当前密码相同");
        }
        String encoded = passwordEncoder.encode(newPassword);
        int updated = jdbcTemplate.update("""
                        update local_users set password_hash = ?, must_change_password = false, updated_at = ?
                        where id = ? and tenant_id = ? and password_hash = ? and enabled = true
                        """, encoded, Timestamp.from(clock.instant()), principal.id(), principal.tenantId(),
                principal.password());
        if (updated != 1) {
            throw new ApplicationException("PASSWORD_CHANGE_CONFLICT", "账号密码已发生变化，请重新登录后再试");
        }
        auditRecorder.record(new ActorContext(principal.tenantId(), principal.username()),
                "LOCAL_PASSWORD_CHANGED", "LOCAL_USER", principal.id(), Map.of());
    }

    private void validateNewPassword(String username, String password, String confirmation) {
        if (password == null || password.length() < 8 || password.length() > 128) {
            throw new ApplicationException("PASSWORD_POLICY_REJECTED", "新密码必须为 8 到 128 个字符");
        }
        if (!password.equals(confirmation)) {
            throw new ApplicationException("PASSWORD_CONFIRMATION_MISMATCH", "两次输入的新密码不一致");
        }
        String normalized = password.toLowerCase(Locale.ROOT);
        if (DISALLOWED.contains(normalized) || normalized.contains(username.toLowerCase(Locale.ROOT))) {
            throw new ApplicationException("PASSWORD_POLICY_REJECTED", "新密码不能使用常见密码或包含用户名");
        }
        boolean lower = password.chars().anyMatch(Character::isLowerCase);
        boolean upper = password.chars().anyMatch(Character::isUpperCase);
        boolean digit = password.chars().anyMatch(Character::isDigit);
        boolean special = password.chars().anyMatch(value -> !Character.isLetterOrDigit(value));
        if (!lower || !upper || !digit || !special) {
            throw new ApplicationException("PASSWORD_POLICY_REJECTED", "新密码必须同时包含大写字母、小写字母、数字和特殊字符");
        }
    }
}
