package io.contentpublisher.platform.infrastructure.config;

import io.contentpublisher.platform.application.ProjectApplicationService;
import io.contentpublisher.platform.application.JobApplicationService;
import io.contentpublisher.platform.application.PublishingApplicationService;
import io.contentpublisher.platform.application.port.ArticleRepository;
import io.contentpublisher.platform.application.port.AuditRecorder;
import io.contentpublisher.platform.application.port.ContentGenerator;
import io.contentpublisher.platform.application.port.ProjectRepository;
import io.contentpublisher.platform.application.port.RepositoryInspector;
import io.contentpublisher.platform.application.port.RepositorySnapshotStore;
import io.contentpublisher.platform.application.port.JobRepository;
import io.contentpublisher.platform.application.port.ChannelAccountRepository;
import io.contentpublisher.platform.application.port.PublicationRepository;
import io.contentpublisher.platform.application.port.CredentialVault;
import io.contentpublisher.platform.application.port.ChannelEndpointPolicy;
import io.contentpublisher.platform.application.port.ChannelPublisher;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.net.http.HttpClient;
import java.time.Clock;
import java.util.List;

@Configuration
@EnableScheduling
@EnableConfigurationProperties({GitImportProperties.class, AiProperties.class, JobProperties.class, ChannelProperties.class})
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
    HttpClient channelHttpClient(ChannelProperties properties) {
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
    PublishingApplicationService publishingApplicationService(ArticleRepository articles,
                                                               ChannelAccountRepository accounts,
                                                               PublicationRepository publications,
                                                               CredentialVault credentialVault,
                                                               ChannelEndpointPolicy endpointPolicy,
                                                               List<ChannelPublisher> publishers,
                                                               AuditRecorder auditRecorder,
                                                               Clock clock) {
        return new PublishingApplicationService(articles, accounts, publications, credentialVault,
                endpointPolicy, publishers, auditRecorder, clock);
    }

    @Bean
    JobApplicationService jobApplicationService(JobRepository jobs, ProjectApplicationService projects,
                                                PublishingApplicationService publishing,
                                                AuditRecorder auditRecorder, Clock clock, JobProperties properties) {
        return new JobApplicationService(jobs, projects, publishing, auditRecorder, clock,
                properties.maxActiveJobsPerTenant(), properties.maxAttempts());
    }
}
