package io.contentpublisher.platform;

import io.contentpublisher.platform.application.port.AuditRecorder;
import io.contentpublisher.platform.application.port.ProjectRepository;
import io.contentpublisher.platform.application.port.ArticleRepository;
import io.contentpublisher.platform.application.port.ChannelAccountRepository;
import io.contentpublisher.platform.domain.ActorContext;
import io.contentpublisher.platform.domain.Article;
import io.contentpublisher.platform.domain.ArticleStatus;
import io.contentpublisher.platform.domain.ArticleVersion;
import io.contentpublisher.platform.domain.ChannelAccount;
import io.contentpublisher.platform.domain.ChannelAccountStatus;
import io.contentpublisher.platform.domain.ChannelType;
import io.contentpublisher.platform.domain.ContentOrigin;
import io.contentpublisher.platform.domain.TopicBrief;
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
    @Autowired ArticleRepository articles;
    @Autowired ChannelAccountRepository channelAccounts;
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

    @Test
    void shouldPersistArticleAndImmutableVersionTogether() {
        Project project = project("tenant-version", "https://github.com/contentpublisher/versioned.git");
        projects.save(project);
        Instant now = Instant.parse("2026-07-20T00:00:00Z");
        Article article = new Article(UUID.randomUUID(), "tenant-version", ContentOrigin.git(project.id()), null,
                "Title", "Summary", "## Body", List.of("Java", "Publishing"),
                List.of("Java publishing tutorial", "content workflow"), "en", "abc123", 1,
                ArticleStatus.DRAFT, "editor", "editor", now, now);

        articles.saveWithVersion(article, new ArticleVersion("tenant-version", article.id(), 1,
                article.title(), article.summary(), article.markdown(), article.tags(), article.keywords(), "editor", now));

        assertThat(articles.findArticleById("tenant-version", article.id())).get().satisfies(saved -> {
            assertThat(saved.currentVersion()).isEqualTo(1);
            assertThat(saved.tags()).containsExactly("Java", "Publishing");
            assertThat(saved.keywords()).containsExactly("Java publishing tutorial", "content workflow");
        });
        assertThat(articles.findVersions("tenant-version", article.id())).singleElement().satisfies(version -> {
            assertThat(version.versionNumber()).isEqualTo(1);
            assertThat(version.tags()).containsExactly("Java", "Publishing");
            assertThat(version.keywords()).containsExactly("Java publishing tutorial", "content workflow");
        });
        assertThat(articles.findVersions("other-tenant", article.id())).isEmpty();
    }

    @Test
    void shouldPersistTopicArticleWithoutGitProject() {
        Instant now = Instant.parse("2026-07-20T00:00:00Z");
        TopicBrief brief = new TopicBrief("PostgreSQL 索引优化", "创建面向开发者的排查教程", "后端开发者",
                "TROUBLESHOOTING", "INTERMEDIATE", List.of("PostgreSQL", "索引"), null);
        Article article = new Article(UUID.randomUUID(), "tenant-topic", ContentOrigin.topic(brief), UUID.randomUUID(),
                "PostgreSQL 索引优化教程", "摘要", "## 分步教程\n正文", brief.keywords(), "zh-CN",
                "b".repeat(64), 1, ArticleStatus.DRAFT, "editor", "editor", now, now);

        articles.saveWithVersion(article, new ArticleVersion("tenant-topic", article.id(), 1, article.title(),
                article.summary(), article.markdown(), article.keywords(), "editor", now));

        assertThat(articles.findArticleById("tenant-topic", article.id())).get().satisfies(saved -> {
            assertThat(saved.projectId()).isNull();
            assertThat(saved.sourceType().name()).isEqualTo("TOPIC");
            assertThat(saved.origin().title()).isEqualTo("PostgreSQL 索引优化");
            assertThat(saved.origin().requestedKeywords()).containsExactly("PostgreSQL", "索引");
        });
    }

    @Test
    void shouldAtomicallyRejectStaleChannelAccountVersion() {
        Instant now = Instant.parse("2026-07-20T00:00:00Z");
        ChannelAccount account = new ChannelAccount(UUID.randomUUID(), "tenant-account-version", ChannelType.DEV,
                "DEV", "https://dev.to", "v1:encrypted", "account-version-key", "a".repeat(64),
                "b".repeat(64), 1, ChannelAccountStatus.ACTIVE, "admin", "admin", now, now);
        channelAccounts.save(account);
        ChannelAccount disabled = new ChannelAccount(account.id(), account.tenantId(), account.type(),
                account.displayName(), account.baseUrl(), account.encryptedCredentials(), account.idempotencyKey(),
                account.requestHash(), account.credentialFingerprint(), 2, ChannelAccountStatus.DISABLED,
                account.createdBy(), "admin-1", account.createdAt(), now.plusSeconds(1));
        ChannelAccount staleRotation = new ChannelAccount(account.id(), account.tenantId(), account.type(),
                account.displayName(), account.baseUrl(), "v1:other", account.idempotencyKey(), account.requestHash(),
                "c".repeat(64), 2, account.status(), account.createdBy(), "admin-2", account.createdAt(),
                now.plusSeconds(2));

        assertThat(channelAccounts.updateIfVersionMatches(disabled, 1)).get()
                .extracting(ChannelAccount::status).isEqualTo(ChannelAccountStatus.DISABLED);
        assertThat(channelAccounts.updateIfVersionMatches(staleRotation, 1)).isEmpty();
        assertThat(channelAccounts.findChannelAccountById(account.tenantId(), account.id())).get()
                .extracting(ChannelAccount::version, ChannelAccount::status)
                .containsExactly(2, ChannelAccountStatus.DISABLED);
    }

    private Project project(String tenantId, String gitUrl) {
        Instant now = Instant.now();
        return new Project(UUID.randomUUID(), tenantId, gitUrl, "platform", "publisher", "main", "abc123",
                List.of("Java"), "LICENSE", ProjectStatus.READY, "creator", "creator", now, now);
    }
}
