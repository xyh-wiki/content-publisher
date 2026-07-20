package io.contentpublisher.platform.infrastructure.config;

import io.contentpublisher.platform.application.ProjectApplicationService;
import io.contentpublisher.platform.application.JobApplicationService;
import io.contentpublisher.platform.application.port.ArticleRepository;
import io.contentpublisher.platform.application.port.AuditRecorder;
import io.contentpublisher.platform.application.port.ContentGenerator;
import io.contentpublisher.platform.application.port.ProjectRepository;
import io.contentpublisher.platform.application.port.RepositoryInspector;
import io.contentpublisher.platform.application.port.RepositorySnapshotStore;
import io.contentpublisher.platform.application.port.JobRepository;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.net.http.HttpClient;
import java.time.Clock;

@Configuration
@EnableScheduling
@EnableConfigurationProperties({GitImportProperties.class, AiProperties.class, JobProperties.class})
public class InfrastructureConfiguration {
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    HttpClient aiHttpClient(AiProperties properties) {
        return HttpClient.newBuilder().connectTimeout(properties.timeout()).build();
    }

    @Bean
    ProjectApplicationService projectApplicationService(ProjectRepository projects,
                                                         ArticleRepository articles,
                                                         RepositorySnapshotStore snapshots,
                                                         RepositoryInspector inspector,
                                                         ContentGenerator generator,
                                                         AuditRecorder auditRecorder,
                                                         Clock clock) {
        return new ProjectApplicationService(projects, articles, snapshots, inspector, generator, auditRecorder, clock);
    }

    @Bean
    JobApplicationService jobApplicationService(JobRepository jobs, ProjectApplicationService projects,
                                                AuditRecorder auditRecorder, Clock clock, JobProperties properties) {
        return new JobApplicationService(jobs, projects, auditRecorder, clock,
                properties.maxActiveJobsPerTenant(), properties.maxAttempts());
    }
}
