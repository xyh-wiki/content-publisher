package io.contentpublisher.platform;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import org.springframework.security.core.authority.SimpleGrantedAuthority;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:security;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "publisher.security.enabled=true",
        "publisher.jobs.worker-enabled=false",
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://127.0.0.1/unused"
})
@AutoConfigureMockMvc
class SecurityIntegrationTest {
    @Autowired MockMvc mockMvc;

    @Test
    void shouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/projects/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAllowViewerToReadWithinTenantContext() throws Exception {
        mockMvc.perform(get("/api/v1/projects/" + UUID.randomUUID())
                        .with(jwt().jwt(token -> token.subject("reader")
                                .claim("tenant_id", "tenant-a")
                                .claim("roles", List.of("VIEWER")))
                                .authorities(new SimpleGrantedAuthority("ROLE_VIEWER"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldDenyViewerFromImportingRepository() throws Exception {
        mockMvc.perform(post("/api/v1/projects/imports")
                        .contentType("application/json")
                        .content("{\"gitUrl\":\"https://github.com/example/repository.git\"}")
                        .with(jwt().jwt(token -> token.subject("reader")
                                .claim("tenant_id", "tenant-a")
                                .claim("roles", List.of("VIEWER")))
                                .authorities(new SimpleGrantedAuthority("ROLE_VIEWER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAcceptEditorJobSubmission() throws Exception {
        mockMvc.perform(post("/api/v1/projects/imports")
                        .header("Idempotency-Key", "security-test-import-001")
                        .contentType("application/json")
                        .content("{\"gitUrl\":\"https://github.com/contentpublisher/platform.git\"}")
                        .with(jwt().jwt(token -> token.subject("editor")
                                        .claim("tenant_id", "tenant-a")
                                        .claim("roles", List.of("EDITOR")))
                                .authorities(new SimpleGrantedAuthority("ROLE_EDITOR"))))
                .andExpect(status().isAccepted())
                .andExpect(header().string("Location", org.hamcrest.Matchers.startsWith("/api/v1/jobs/")))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.type").value("IMPORT_PROJECT"));
    }

    @Test
    void shouldRejectWriteRequestWithoutIdempotencyKey() throws Exception {
        mockMvc.perform(post("/api/v1/projects/imports")
                        .contentType("application/json")
                        .content("{\"gitUrl\":\"https://github.com/contentpublisher/platform.git\"}")
                        .with(jwt().jwt(token -> token.subject("editor")
                                        .claim("tenant_id", "tenant-missing-header")
                                        .claim("roles", List.of("EDITOR")))
                                .authorities(new SimpleGrantedAuthority("ROLE_EDITOR"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REQUEST_HEADER_MISSING"));
    }
}
