package io.contentpublisher.platform;

import io.contentpublisher.platform.application.port.AuditRecorder;
import io.contentpublisher.platform.application.port.ProjectRepository;
import io.contentpublisher.platform.domain.ActorContext;
import io.contentpublisher.platform.domain.Project;
import io.contentpublisher.platform.domain.ProjectStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:tenant;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "publisher.security.enabled=false"
})
class TenantPersistenceIntegrationTest {
    @Autowired ProjectRepository projects;
    @Autowired AuditRecorder auditRecorder;
    @Autowired JdbcTemplate jdbcTemplate;

    @Test
    void shouldIsolateSameRepositoryAcrossTenantsAndWriteAuditLog() {
        String gitUrl = "https://github.com/contentpublisher/platform.git";
        Project tenantA = project("tenant-a", gitUrl);
        Project tenantB = project("tenant-b", gitUrl);
        projects.save(tenantA);
        projects.save(tenantB);

        assertThat(projects.findByGitUrl("tenant-a", gitUrl)).get().extracting(Project::id).isEqualTo(tenantA.id());
        assertThat(projects.findProjectById("tenant-b", tenantA.id())).isEmpty();

        auditRecorder.record(new ActorContext("tenant-a", "editor-a"), "PROJECT_IMPORTED", "PROJECT",
                tenantA.id(), Map.of("revision", "abc123"));
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from audit_logs where tenant_id = ? and target_id = ?", Integer.class,
                "tenant-a", tenantA.id());
        assertThat(count).isEqualTo(1);
    }

    private Project project(String tenantId, String gitUrl) {
        Instant now = Instant.now();
        return new Project(UUID.randomUUID(), tenantId, gitUrl, "platform", "publisher", "main", "abc123",
                List.of("Java"), "LICENSE", ProjectStatus.READY, "creator", "creator", now, now);
    }
}
