package com.codevision.codevisionbackend.dependency;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
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
import org.springframework.stereotype.Component;

/**
 * Resolves full transitive Maven dependency trees by parsing output of
 * {@code mvn dependency:tree -DoutputType=text}.
 */
@Component
public class MavenDependencyResolver implements DependencyResolver {

    private static final Logger log = LoggerFactory.getLogger(MavenDependencyResolver.class);
    private static final Pattern TREE_LINE_PATTERN = Pattern.compile(
            "^([| +-\\\\]*)\\s*([^:]+):([^:]+):([^:]+):([^:]+):([^:]+)(?::([^:]+))?$");
    private static final Pattern SIMPLE_PATTERN = Pattern.compile(
            "^([| +-\\\\]*)\\s*([^:]+):([^:]+):([^:]+):([^:]+)$");
    private static final long DEFAULT_PROCESS_TIMEOUT_SECONDS = 120;

    @Override
    public boolean supports(Path projectRoot) {
        return Files.isRegularFile(projectRoot.resolve("pom.xml"));
    }

    @Override
    public String buildSystem() {
        return "maven";
    }

    @Override
    public Optional<DependencyTree> resolve(Path projectRoot, ExclusionConfig exclusionConfig) {
        try {
            List<String> treeOutput = runMavenDependencyTree(projectRoot);
            if (treeOutput.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(parseTreeOutput(treeOutput, exclusionConfig));
        } catch (IOException | InterruptedException ex) {
            log.error("Failed to resolve Maven dependencies at {}: {}", projectRoot, ex.getMessage());
            return Optional.empty();
        }
    }

    List<String> runMavenDependencyTree(Path projectRoot) throws IOException, InterruptedException {
        var process = new ProcessBuilder("mvn", "dependency:tree", "-DoutputType=text", "-q")
                .directory(projectRoot.toFile())
                .redirectErrorStream(true)
                .start();
        List<String> output;
        try (var reader = process.getInputStream()) {
            output = new String(reader.readAllBytes()).lines().toList();
        }
        boolean finished = process.waitFor(DEFAULT_PROCESS_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            log.warn("mvn dependency:tree timed out after {}s at {}", DEFAULT_PROCESS_TIMEOUT_SECONDS, projectRoot);
            return List.of();
        }
        int exitCode = process.exitValue();
        if (exitCode != 0) {
            log.warn("mvn dependency:tree exited with code {} at {}", exitCode, projectRoot);
            return List.of();
        }
        return output;
    }

    /**
     * Parses the text output of `mvn dependency:tree` into a DependencyTree.
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
            if (line.isBlank() || line.startsWith("[")) {
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
        // Try 6-part format: group:artifact:type:version:scope:classifier
        Matcher matcher = TREE_LINE_PATTERN.matcher(line);
        if (matcher.matches()) {
            int depth = calculateDepth(matcher.group(1));
            return new ParsedLine(new ResolvedArtifact(
                    matcher.group(2).trim(),
                    matcher.group(3).trim(),
                    matcher.group(5).trim(),
                    matcher.group(6).trim(),
                    matcher.group(4).trim(),
                    matcher.group(7) != null ? matcher.group(7).trim() : null,
                    false), depth);
        }

        // Try 5-part format: group:artifact:type:version:scope (no prefix)
        // Handle root project line like "com.example:my-project:jar:1.0.0"
        Matcher simpleMatcher = SIMPLE_PATTERN.matcher(line);
        if (simpleMatcher.matches()) {
            int depth = calculateDepth(simpleMatcher.group(1));
            return new ParsedLine(new ResolvedArtifact(
                    simpleMatcher.group(2).trim(),
                    simpleMatcher.group(3).trim(),
                    simpleMatcher.group(5).trim(),
                    "compile",
                    simpleMatcher.group(4).trim(),
                    null,
                    false), depth);
        }

        return null;
    }

    private int calculateDepth(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            return 0;
        }
        // Each level of nesting adds approximately 3 characters of tree prefix
        return (prefix.length() + 2) / 3;
    }
}
