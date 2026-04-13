package com.codevision.codevisionbackend.dependency;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Resolves full transitive npm dependency trees by parsing output of
 * {@code npm ls --all --json}.
 */
@Component
public class NpmDependencyResolver implements DependencyResolver {

    private static final Logger log = LoggerFactory.getLogger(NpmDependencyResolver.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final int RECURSION_DEPTH_LIMIT = 200;

    private final long processTimeoutSeconds;

    public NpmDependencyResolver(
            @Value("${codevision.dependency.npm.processTimeoutSeconds:120}") long processTimeoutSeconds) {
        this.processTimeoutSeconds = processTimeoutSeconds;
    }

    /** No-arg constructor with default timeout for test convenience. */
    public NpmDependencyResolver() {
        this(120);
    }
    private static final ResolvedArtifact UNKNOWN_ROOT =
            new ResolvedArtifact("unknown", "unknown", "0.0.0");

    @Override
    public boolean supports(Path projectRoot) {
        return Files.isRegularFile(projectRoot.resolve("package.json"));
    }

    @Override
    public String buildSystem() {
        return "npm";
    }

    @Override
    public Optional<DependencyTree> resolve(Path projectRoot, ExclusionConfig exclusionConfig) {
        try {
            var jsonOutput = runNpmLs(projectRoot);
            if (jsonOutput == null || jsonOutput.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(parseJsonOutput(jsonOutput, exclusionConfig));
        } catch (IOException | InterruptedException ex) {
            log.error("Failed to resolve npm dependencies at {}: {}", projectRoot, ex.getMessage());
            return Optional.empty();
        }
    }

    String runNpmLs(Path projectRoot) throws IOException, InterruptedException {
        var process = new ProcessBuilder("npm", "ls", "--all", "--json")
                .directory(projectRoot.toFile())
                .redirectErrorStream(true)
                .start();
        // Read stream BEFORE waitFor to prevent pipe deadlock
        String output;
        try (var reader = process.getInputStream()) {
            output = new String(reader.readAllBytes());
        }
        boolean finished = process.waitFor(processTimeoutSeconds, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            log.warn("npm ls timed out after {}s at {}", processTimeoutSeconds, projectRoot);
            return null;
        }
        // npm ls exits with code 1 for peer dep warnings but still produces valid JSON
        int exitCode = process.exitValue();
        if (exitCode != 0 && exitCode != 1) {
            log.warn("npm ls exited with code {} at {}", exitCode, projectRoot);
            return null;
        }
        return output;
    }

    /**
     * Parses the JSON output of {@code npm ls --all --json} into a DependencyTree.
     * Package-private for testability.
     */
    DependencyTree parseJsonOutput(String json, ExclusionConfig exclusionConfig) {
        if (json == null || json.isBlank()) {
            return new DependencyTree(UNKNOWN_ROOT);
        }

        JsonNode rootNode;
        try {
            rootNode = OBJECT_MAPPER.readTree(json);
        } catch (IOException ex) {
            log.warn("Failed to parse npm ls JSON output: {}", ex.getMessage());
            return new DependencyTree(UNKNOWN_ROOT);
        }

        var name = rootNode.has("name") ? rootNode.get("name").asText() : null;
        var version = rootNode.has("version") ? rootNode.get("version").asText() : null;

        if (name == null || version == null) {
            return new DependencyTree(UNKNOWN_ROOT);
        }

        var rootArtifact = createArtifact(name, version);
        var root = new DependencyTree(rootArtifact);
        var visited = new HashSet<String>();
        visited.add(rootArtifact.coordinates());

        if (rootNode.has("dependencies")) {
            processDependencies(rootNode.get("dependencies"), root, exclusionConfig, visited, 0);
        }

        return root;
    }

    private void processDependencies(JsonNode dependenciesNode, DependencyTree parent,
                                     ExclusionConfig exclusionConfig, Set<String> visited, int depth) {
        if (depth > RECURSION_DEPTH_LIMIT) {
            log.warn("npm dependency tree exceeded maximum recursion depth of {}, truncating", RECURSION_DEPTH_LIMIT);
            return;
        }
        var fieldNames = dependenciesNode.fieldNames();
        while (fieldNames.hasNext()) {
            var packageName = fieldNames.next();
            var packageNode = dependenciesNode.get(packageName);

            var version = packageNode.has("version") ? packageNode.get("version").asText() : "0.0.0";
            var artifact = createArtifact(packageName, version);

            if (visited.contains(artifact.coordinates())) {
                continue;
            }

            if (exclusionConfig != null && exclusionConfig.isExcluded(artifact)) {
                continue;
            }

            visited.add(artifact.coordinates());
            var childTree = new DependencyTree(artifact);
            parent.addChild(childTree);

            if (packageNode.has("dependencies")) {
                processDependencies(packageNode.get("dependencies"), childTree, exclusionConfig, visited, depth + 1);
            }
        }
    }

    /**
     * Creates a ResolvedArtifact from an npm package name.
     * Scoped packages (e.g., {@code @types/react}) use the scope as groupId
     * and the package name after the slash as artifactId.
     * Unscoped packages use the package name as both groupId and artifactId.
     */
    private static ResolvedArtifact createArtifact(String packageName, String version) {
        if (packageName.startsWith("@") && packageName.contains("/")) {
            var parts = packageName.split("/", 2);
            return new ResolvedArtifact(parts[0], parts[1], version, "compile", "npm", null, false);
        }
        return new ResolvedArtifact(packageName, packageName, version, "compile", "npm", null, false);
    }
}
