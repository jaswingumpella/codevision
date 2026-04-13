package com.codevision.codevisionbackend.dependency;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Resolves full transitive Gradle dependency trees by parsing output of
 * {@code gradle dependencies --configuration runtimeClasspath -q}.
 */
@Component
public class GradleDependencyResolver implements DependencyResolver {

    private static final Logger log = LoggerFactory.getLogger(GradleDependencyResolver.class);

    /**
     * Matches Gradle dependency tree lines like:
     * {@code +--- group:artifact:version} or {@code \--- group:artifact:version}
     * Also handles version conflict notation: {@code group:artifact:1.0 -> 2.0}
     * and omitted subtree marker: {@code group:artifact:1.0 (*)}
     */
    private static final Pattern TREE_LINE_PATTERN = Pattern.compile(
            "^([| +-\\\\]*)([^:]+):([^:]+):([^:\\s]+)(?:\\s+->\\s+([^\\s(]+))?(?:\\s+\\(\\*\\))?\\s*$");

    /**
     * Matches root project line: {@code group:artifact:version}
     */
    private static final Pattern ROOT_PATTERN = Pattern.compile(
            "^([^:]+):([^:]+):([^:\\s]+)\\s*$");

    private final long processTimeoutSeconds;

    public GradleDependencyResolver(
            @Value("${codevision.dependency.gradle.processTimeoutSeconds:120}") long processTimeoutSeconds) {
        this.processTimeoutSeconds = processTimeoutSeconds;
    }

    /** No-arg constructor with default timeout for test convenience. */
    public GradleDependencyResolver() {
        this(120);
    }

    @Override
    public boolean supports(Path projectRoot) {
        return Files.isRegularFile(projectRoot.resolve("build.gradle"))
                || Files.isRegularFile(projectRoot.resolve("build.gradle.kts"));
    }

    @Override
    public String buildSystem() {
        return "gradle";
    }

    @Override
    public Optional<DependencyTree> resolve(Path projectRoot, ExclusionConfig exclusionConfig) {
        try {
            List<String> treeOutput = runGradleDependencyTree(projectRoot);
            if (treeOutput.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(parseTreeOutput(treeOutput, exclusionConfig));
        } catch (IOException | InterruptedException ex) {
            log.error("Failed to resolve Gradle dependencies at {}: {}", projectRoot, ex.getMessage());
            return Optional.empty();
        }
    }

    List<String> runGradleDependencyTree(Path projectRoot) throws IOException, InterruptedException {
        var process = new ProcessBuilder("gradle", "dependencies", "--configuration", "runtimeClasspath", "-q")
                .directory(projectRoot.toFile())
                .redirectErrorStream(true)
                .start();
        // Read stream BEFORE waitFor to prevent pipe deadlock
        List<String> output;
        try (var reader = process.getInputStream()) {
            output = new String(reader.readAllBytes()).lines().toList();
        }
        boolean finished = process.waitFor(processTimeoutSeconds, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            log.warn("gradle dependencies timed out after {}s at {}", processTimeoutSeconds, projectRoot);
            return List.of();
        }
        int exitCode = process.exitValue();
        if (exitCode != 0) {
            log.warn("gradle dependencies exited with code {} at {}", exitCode, projectRoot);
            return List.of();
        }
        return output;
    }

    /**
     * Parses the text output of {@code gradle dependencies} into a DependencyTree.
     * Visible for testing.
     */
    DependencyTree parseTreeOutput(List<String> lines, ExclusionConfig exclusionConfig) {
        if (lines.isEmpty()) {
            return new DependencyTree(new ResolvedArtifact("unknown", "unknown", "0.0.0"));
        }

        DependencyTree root = null;
        Deque<DependencyTree> stack = new ArrayDeque<>();
        Deque<Integer> depthStack = new ArrayDeque<>();
        Set<String> seen = new HashSet<>();

        for (String line : lines) {
            if (line.isBlank()) {
                continue;
            }

            var parsed = parseLine(line);
            if (parsed == null) {
                continue;
            }

            ResolvedArtifact artifact = parsed.artifact;
            int depth = parsed.depth;

            if (root == null) {
                root = new DependencyTree(artifact);
                stack.push(root);
                depthStack.push(-1);
                seen.add(artifact.coordinates());
                continue;
            }

            if (seen.contains(artifact.coordinates())) {
                continue;
            }

            if (exclusionConfig != null && exclusionConfig.isExcluded(artifact)) {
                continue;
            }

            seen.add(artifact.coordinates());
            var node = new DependencyTree(artifact);

            while (!depthStack.isEmpty() && depthStack.peek() >= depth) {
                stack.pop();
                depthStack.pop();
            }

            if (!stack.isEmpty()) {
                stack.peek().addChild(node);
            }

            stack.push(node);
            depthStack.push(depth);
        }

        return root != null ? root : new DependencyTree(new ResolvedArtifact("unknown", "unknown", "0.0.0"));
    }

    private record ParsedLine(ResolvedArtifact artifact, int depth) {}

    private ParsedLine parseLine(String line) {
        // Try tree line with prefix (+--- or \---)
        Matcher treeMatcher = TREE_LINE_PATTERN.matcher(line);
        if (treeMatcher.matches()) {
            int depth = calculateDepth(treeMatcher.group(1));
            String group = treeMatcher.group(2).trim();
            String artifactId = treeMatcher.group(3).trim();
            // If there's a version conflict resolution (->), use the resolved version
            String version = treeMatcher.group(5) != null
                    ? treeMatcher.group(5).trim()
                    : treeMatcher.group(4).trim();
            return new ParsedLine(new ResolvedArtifact(group, artifactId, version), depth);
        }

        // Try root line: group:artifact:version
        Matcher rootMatcher = ROOT_PATTERN.matcher(line);
        if (rootMatcher.matches()) {
            return new ParsedLine(new ResolvedArtifact(
                    rootMatcher.group(1).trim(),
                    rootMatcher.group(2).trim(),
                    rootMatcher.group(3).trim()), 0);
        }

        return null;
    }

    private int calculateDepth(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            return 0;
        }
        // Gradle uses 5-character groups per depth level: "+--- " or "|    "
        return (prefix.length() + 4) / 5;
    }
}
