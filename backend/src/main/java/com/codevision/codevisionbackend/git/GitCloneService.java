package com.codevision.codevisionbackend.git;

import com.codevision.codevisionbackend.config.GitAuthProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class GitCloneService {

    private static final Logger log = LoggerFactory.getLogger(GitCloneService.class);
    private final GitAuthProperties gitAuthProperties;

    public GitCloneService(GitAuthProperties gitAuthProperties) {
        this.gitAuthProperties = gitAuthProperties;
    }

    public CloneResult cloneRepository(String repoUrl) {
        String projectName = deriveProjectName(repoUrl);
        Path workingDir = createWorkingDirectory(projectName);

        try {
            var cloneCommand = Git.cloneRepository()
                    .setURI(repoUrl)
                    .setDirectory(workingDir.toFile());

            CredentialsProvider credentialsProvider = resolveCredentialsProvider();
            if (credentialsProvider != null) {
                cloneCommand.setCredentialsProvider(credentialsProvider);
            }

            cloneCommand.call().close();
            log.info("Cloned repository {} into {}", repoUrl, workingDir);
            return new CloneResult(projectName, workingDir);
        } catch (GitAPIException e) {
            deleteDirectoryQuietly(workingDir);
            throw new IllegalStateException("Failed to clone repository", e);
        }
    }

    public void cleanupClone(Path directory) {
        deleteDirectoryQuietly(directory);
    }

    public void cleanupClone(CloneResult cloneResult) {
        if (cloneResult == null) {
            return;
        }
        cleanupClone(cloneResult.directory());
    }

    private CredentialsProvider resolveCredentialsProvider() {
        String username = gitAuthProperties.getUsername();
        String token = gitAuthProperties.getToken();
        if (username == null || username.isBlank() || token == null || token.isBlank()) {
            return null;
        }
        return new UsernamePasswordCredentialsProvider(username, token);
    }

    private String deriveProjectName(String repoUrl) {
        String sanitized = repoUrl;
        if (sanitized.endsWith("/")) {
            sanitized = sanitized.substring(0, sanitized.length() - 1);
        }
        int lastSlash = sanitized.lastIndexOf('/') + 1;
        String raw = sanitized.substring(lastSlash);
        if (raw.endsWith(".git")) {
            raw = raw.substring(0, raw.length() - 4);
        }
        return raw;
    }

    private Path createWorkingDirectory(String projectName) {
        try {
            return Files.createTempDirectory("codevision-" + projectName + "-");
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create working directory", e);
        }
    }

    private void deleteDirectoryQuietly(Path directory) {
        if (directory == null) {
            return;
        }
        try (var paths = Files.walk(directory)) {
            paths.sorted((a, b) -> b.compareTo(a)).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ex) {
                    log.warn("Failed to delete temporary path {}", path, ex);
                }
            });
        } catch (IOException ex) {
            log.warn("Failed to traverse temporary directory {}", directory, ex);
        }
    }

    public record CloneResult(String projectName, Path directory) {
    }
}
