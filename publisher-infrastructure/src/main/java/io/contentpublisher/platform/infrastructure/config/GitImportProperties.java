package io.contentpublisher.platform.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;
import java.util.Set;

@ConfigurationProperties("publisher.git")
public record GitImportProperties(
        Path workDirectory,
        Set<String> allowedHosts,
        int timeoutSeconds,
        long maxRepositoryBytes,
        int maxFiles,
        int maxReadmeCharacters) {

    public GitImportProperties {
        workDirectory = workDirectory == null ? Path.of("/data/tmp/content-publisher") : workDirectory;
        allowedHosts = allowedHosts == null || allowedHosts.isEmpty()
                ? Set.of("github.com", "gitlab.com", "gitee.com") : Set.copyOf(allowedHosts);
        timeoutSeconds = timeoutSeconds <= 0 ? 30 : timeoutSeconds;
        maxRepositoryBytes = maxRepositoryBytes <= 0 ? 100L * 1024 * 1024 : maxRepositoryBytes;
        maxFiles = maxFiles <= 0 ? 2_000 : maxFiles;
        maxReadmeCharacters = maxReadmeCharacters <= 0 ? 60_000 : maxReadmeCharacters;
    }
}
