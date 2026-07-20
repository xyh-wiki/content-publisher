package io.contentpublisher.platform.infrastructure.git;

import io.contentpublisher.platform.application.ApplicationException;
import io.contentpublisher.platform.application.port.RepositoryInspector;
import io.contentpublisher.platform.domain.RepositorySnapshot;
import io.contentpublisher.platform.infrastructure.config.GitImportProperties;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

@Component
public class SecureJGitRepositoryInspector implements RepositoryInspector {
    private static final List<String> README_NAMES = List.of("README.md", "README.MD", "README", "readme.md");
    private static final List<String> MANIFEST_NAMES = List.of(
            "pom.xml", "package.json", "pyproject.toml", "requirements.txt", "Cargo.toml", "go.mod", "build.gradle", "build.gradle.kts");
    private static final Map<String, String> LANGUAGES = languageMap();

    private final GitImportProperties properties;

    public SecureJGitRepositoryInspector(GitImportProperties properties) {
        this.properties = properties;
    }

    @Override
    public RepositorySnapshot inspect(String gitUrl, String branch) {
        URI uri = validateUri(gitUrl);
        Path cloneDirectory = null;
        try {
            Files.createDirectories(properties.workDirectory());
            cloneDirectory = Files.createTempDirectory(properties.workDirectory(), "repo-");
            var command = Git.cloneRepository()
                    .setURI(uri.toString())
                    .setDirectory(cloneDirectory.toFile())
                    .setDepth(1)
                    .setCloneSubmodules(false)
                    .setTimeout(properties.timeoutSeconds());
            if (branch != null && !branch.isBlank()) {
                command.setBranch(branch.trim());
            }
            try (Git git = command.call()) {
                enforceRepositoryLimits(cloneDirectory);
                String revision = git.getRepository().resolve("HEAD").name();
                String resolvedBranch = git.getRepository().getBranch();
                String readme = readFirstExisting(cloneDirectory, README_NAMES, properties.maxReadmeCharacters());
                String manifest = readManifests(cloneDirectory);
                List<String> tree = fileTree(cloneDirectory);
                return new RepositorySnapshot(repositoryName(uri), description(readme), resolvedBranch, revision,
                        readme, manifest, tree, detectLanguages(tree), detectLicense(cloneDirectory));
            }
        } catch (GitAPIException | IOException exception) {
            throw new ApplicationException("GIT_IMPORT_FAILED", "Git 仓库拉取或分析失败: " + safeMessage(exception), exception);
        } finally {
            deleteRecursively(cloneDirectory);
        }
    }

    private URI validateUri(String gitUrl) {
        try {
            URI uri = URI.create(gitUrl);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) {
                throw new ApplicationException("GIT_URL_REJECTED", "仅允许使用 HTTPS Git 地址");
            }
            String host = uri.getHost().toLowerCase(Locale.ROOT);
            if (!properties.allowedHosts().contains(host)) {
                throw new ApplicationException("GIT_HOST_REJECTED", "Git 主机未加入允许列表: " + host);
            }
            for (InetAddress address : InetAddress.getAllByName(host)) {
                if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                        || address.isSiteLocalAddress() || address.isMulticastAddress()) {
                    throw new ApplicationException("GIT_ADDRESS_REJECTED", "Git 地址解析到非公网网络");
                }
            }
            return uri;
        } catch (IllegalArgumentException | IOException exception) {
            if (exception instanceof ApplicationException applicationException) {
                throw applicationException;
            }
            throw new ApplicationException("GIT_URL_INVALID", "Git 地址格式无效", exception);
        }
    }

    private void enforceRepositoryLimits(Path root) throws IOException {
        long bytes = 0;
        int files = 0;
        try (Stream<Path> stream = Files.walk(root)) {
            for (Path path : stream.filter(Files::isRegularFile).toList()) {
                if (path.startsWith(root.resolve(".git"))) continue;
                files++;
                bytes += Files.size(path);
                if (files > properties.maxFiles() || bytes > properties.maxRepositoryBytes()) {
                    throw new ApplicationException("GIT_REPOSITORY_TOO_LARGE", "仓库超过允许的文件数量或体积限制");
                }
            }
        }
    }

    private List<String> fileTree(Path root) throws IOException {
        try (Stream<Path> stream = Files.walk(root, 6)) {
            return stream.filter(Files::isRegularFile)
                    .filter(path -> !path.startsWith(root.resolve(".git")))
                    .map(root::relativize).map(Path::toString).sorted().limit(500).toList();
        }
    }

    private String readManifests(Path root) throws IOException {
        StringBuilder result = new StringBuilder();
        for (String name : MANIFEST_NAMES) {
            Path path = root.resolve(name);
            if (Files.isRegularFile(path)) {
                result.append("\n--- ").append(name).append(" ---\n")
                        .append(readLimited(path, 12_000));
            }
        }
        return result.toString();
    }

    private String readFirstExisting(Path root, List<String> names, int limit) throws IOException {
        for (String name : names) {
            Path path = root.resolve(name);
            if (Files.isRegularFile(path)) return readLimited(path, limit);
        }
        return "";
    }

    private String readLimited(Path path, int limit) throws IOException {
        byte[] bytes = Files.readAllBytes(path);
        String text = new String(bytes, 0, Math.min(bytes.length, limit * 4), StandardCharsets.UTF_8);
        return text.length() <= limit ? text : text.substring(0, limit);
    }

    private String description(String readme) {
        return readme.lines().map(String::trim)
                .filter(line -> !line.isBlank() && !line.startsWith("#") && !line.startsWith("![") && !line.startsWith("[!"))
                .findFirst().map(line -> line.length() > 500 ? line.substring(0, 500) : line).orElse("");
    }

    private List<String> detectLanguages(List<String> files) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String file : files) {
            int dot = file.lastIndexOf('.');
            if (dot < 0) continue;
            String language = LANGUAGES.get(file.substring(dot).toLowerCase(Locale.ROOT));
            if (language != null) counts.merge(language, 1, Integer::sum);
        }
        return counts.entrySet().stream().sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .map(Map.Entry::getKey).limit(10).toList();
    }

    private String detectLicense(Path root) {
        for (String name : List.of("LICENSE", "LICENSE.md", "COPYING")) {
            if (Files.isRegularFile(root.resolve(name))) return name;
        }
        return "UNKNOWN";
    }

    private String repositoryName(URI uri) {
        String path = uri.getPath();
        String name = path.substring(path.lastIndexOf('/') + 1);
        return name.endsWith(".git") ? name.substring(0, name.length() - 4) : name;
    }

    private String safeMessage(Exception exception) {
        String message = exception.getMessage();
        return message == null ? exception.getClass().getSimpleName() : message.replaceAll("(?i)(token|password|key)=[^&\\s]+", "$1=***");
    }

    private void deleteRecursively(Path directory) {
        if (directory == null || !Files.exists(directory)) return;
        try (Stream<Path> stream = Files.walk(directory)) {
            for (Path path : stream.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // Temporary cleanup failure is non-fatal and can be handled by scheduled maintenance.
        }
    }

    private static Map<String, String> languageMap() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put(".java", "Java"); map.put(".kt", "Kotlin"); map.put(".go", "Go"); map.put(".rs", "Rust");
        map.put(".py", "Python"); map.put(".js", "JavaScript"); map.put(".ts", "TypeScript"); map.put(".tsx", "TypeScript");
        map.put(".vue", "Vue"); map.put(".cs", "C#"); map.put(".cpp", "C++"); map.put(".c", "C"); map.put(".php", "PHP");
        map.put(".rb", "Ruby"); map.put(".swift", "Swift"); map.put(".scala", "Scala"); map.put(".sh", "Shell");
        return Map.copyOf(map);
    }
}
