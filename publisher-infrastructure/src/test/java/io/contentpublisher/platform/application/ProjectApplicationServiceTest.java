package io.contentpublisher.platform.application;

import io.contentpublisher.platform.application.port.ArticleRepository;
import io.contentpublisher.platform.application.port.AuditRecorder;
import io.contentpublisher.platform.application.port.ContentGenerator;
import io.contentpublisher.platform.application.port.ProjectRepository;
import io.contentpublisher.platform.application.port.RepositoryInspector;
import io.contentpublisher.platform.application.port.RepositorySnapshotStore;
import io.contentpublisher.platform.application.port.WebsiteInspector;
import io.contentpublisher.platform.domain.ActorContext;
import io.contentpublisher.platform.domain.Article;
import io.contentpublisher.platform.domain.GenerationPolicy;
import io.contentpublisher.platform.domain.Project;
import io.contentpublisher.platform.domain.ProjectStatus;
import io.contentpublisher.platform.domain.RepositorySnapshot;
import io.contentpublisher.platform.domain.WebsiteBrief;
import io.contentpublisher.platform.domain.WebsiteSnapshot;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProjectApplicationServiceTest {
    private static final ActorContext ACTOR = new ActorContext("tenant", "editor");
    private static final Instant NOW = Instant.parse("2026-07-20T08:00:00Z");
    private static final GenerationPolicy POLICY = new GenerationPolicy("zh-CN", "专业", 200, 2000, 5,
            List.of(), List.of(), List.of());

    @Test
    void shouldAppendRepositoryAddressToGitGeneratedArticle() {
        Fixture fixture = fixture();
        UUID projectId = UUID.randomUUID();
        String repositoryUrl = "https://github.com/example/content-platform.git";
        Project project = new Project(projectId, ACTOR.tenantId(), repositoryUrl, "content-platform", null,
                "main", "abc123", List.of("Java"), "MIT", ProjectStatus.READY,
                ACTOR.subject(), ACTOR.subject(), NOW, NOW);
        RepositorySnapshot snapshot = new RepositorySnapshot("content-platform", "内容平台", "main", "abc123",
                "README", "pom.xml", List.of("pom.xml"), List.of("Java"), "MIT");
        when(fixture.projects.findProjectById(ACTOR.tenantId(), projectId)).thenReturn(Optional.of(project));
        when(fixture.snapshots.findByProjectId(ACTOR.tenantId(), projectId)).thenReturn(Optional.of(snapshot));
        when(fixture.generator.generate(ACTOR.tenantId(), snapshot, POLICY)).thenReturn(generated("## 项目介绍\n\n正文"));

        Article article = fixture.service.generateArticle(ACTOR, projectId, POLICY);

        assertThat(article.markdown()).contains("## 推荐地址", "项目仓库：<" + repositoryUrl + ">");
        assertThat(article.markdownEn()).contains("## Recommended Link", "Project Repository：<" + repositoryUrl + ">");
        assertThat(article.titleEn()).isEqualTo("Title");
        assertThat(article.hasEnglishContent()).isTrue();
    }

    @Test
    void shouldAppendFinalWebsiteAddressWithoutDuplicatingExistingLink() {
        Fixture fixture = fixture();
        WebsiteBrief brief = new WebsiteBrief("https://example.com", "介绍网站", "开发者", List.of("工具"));
        String finalUrl = "https://www.example.com/product";
        WebsiteSnapshot snapshot = new WebsiteSnapshot(finalUrl, "Example", "开发工具", "公开功能说明");
        when(fixture.websiteInspector.inspect(brief.websiteUrl())).thenReturn(snapshot);
        when(fixture.generator.generateFromWebsite(ACTOR.tenantId(), brief, snapshot, POLICY))
                .thenReturn(generated("## 网站定位\n\n正文已包含 " + finalUrl));

        Article article = fixture.service.generateWebsiteArticle(ACTOR, brief, POLICY, null);

        assertThat(article.markdown()).contains(finalUrl).doesNotContain("## 推荐网站");
    }

    @Test
    void shouldAppendWebsiteAddressWhenModelOmitsIt() {
        Fixture fixture = fixture();
        WebsiteBrief brief = new WebsiteBrief("https://example.com", "介绍网站", "开发者", List.of("工具"));
        WebsiteSnapshot snapshot = new WebsiteSnapshot("https://example.com/official", "Example", "开发工具",
                "公开功能说明");
        when(fixture.websiteInspector.inspect(brief.websiteUrl())).thenReturn(snapshot);
        when(fixture.generator.generateFromWebsite(ACTOR.tenantId(), brief, snapshot, POLICY))
                .thenReturn(generated("## 网站定位\n\n正文"));

        Article article = fixture.service.generateWebsiteArticle(ACTOR, brief, POLICY, null);

        assertThat(article.markdown()).contains("## 推荐网站", "官方网站：<https://example.com/official>");
    }

    private ContentGenerator.GeneratedContent generated(String markdown) {
        return new ContentGenerator.GeneratedContent("标题", "摘要", markdown, List.of("工具"), List.of("工具推荐"),
                "Title", "Summary", "## Introduction\n\nBody", List.of("tools"), List.of("tool recommendation"));
    }

    private Fixture fixture() {
        ProjectRepository projects = mock(ProjectRepository.class);
        ArticleRepository articles = mock(ArticleRepository.class);
        RepositorySnapshotStore snapshots = mock(RepositorySnapshotStore.class);
        RepositoryInspector repositoryInspector = mock(RepositoryInspector.class);
        WebsiteInspector websiteInspector = mock(WebsiteInspector.class);
        ContentGenerator generator = mock(ContentGenerator.class);
        AuditRecorder auditRecorder = mock(AuditRecorder.class);
        when(articles.saveWithVersion(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));
        ProjectApplicationService service = new ProjectApplicationService(projects, articles, snapshots,
                repositoryInspector, websiteInspector, generator, auditRecorder, Clock.fixed(NOW, ZoneOffset.UTC));
        return new Fixture(service, projects, snapshots, websiteInspector, generator);
    }

    private record Fixture(ProjectApplicationService service, ProjectRepository projects,
                           RepositorySnapshotStore snapshots, WebsiteInspector websiteInspector,
                           ContentGenerator generator) {
    }
}
