package com.codevision.codevisionbackend.analysis;

import com.codevision.codevisionbackend.analysis.config.CompiledAnalysisProperties;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Builds the compiled classpath by invoking Maven goals and assembling {@code target/classes} with
 * dependency jars. All operations run without classloading user bytecode.
 */
@Component
public class ClasspathBuilder {

    private static final Logger log = LoggerFactory.getLogger(ClasspathBuilder.class);
    private final CompiledAnalysisProperties properties;
    private final MavenCommandRunner commandRunner;

    public ClasspathBuilder(CompiledAnalysisProperties properties, MavenCommandRunner commandRunner) {
        this.properties = properties;
        this.commandRunner = commandRunner;
    }

    public ClasspathDescriptor build(Path repoRoot, boolean includeDependencies) {
        Objects.requireNonNull(repoRoot, "repoRoot must not be null");
        Path normalizedRoot = repoRoot.toAbsolutePath().normalize();
        Path classesDir = normalizedRoot.resolve("target").resolve("classes");
        if (!Files.isDirectory(classesDir) && properties.getCompile().isAuto()) {
            log.info("Compiled classes missing at {}. Triggering mvn compile.", classesDir);
            runMaven(normalizedRoot, List.of(
                    properties.getCompile().getMvnExecutable(),
                    "-q",
                    "-DskipTests",
                    "compile"));
        }

        if (!Files.isDirectory(classesDir)) {
            throw new IllegalStateException("Classes directory not found at " + classesDir);
        }

        List<Path> entries = new ArrayList<>();
        entries.add(classesDir);
        if (includeDependencies) {
            Path classpathFile = normalizedRoot.resolve("target").resolve("classpath.txt");
            runMaven(normalizedRoot, List.of(
                    properties.getCompile().getMvnExecutable(),
                    "-q",
                    "-DincludeScope=compile",
                    "-DoutputFile=target/classpath.txt",
                    "dependency:build-classpath"));
            entries.addAll(readClasspathEntries(classpathFile));
        }

        List<Pattern> excludePatterns = properties.getFilters().getExcludeJars().stream()
                .filter(Objects::nonNull)
                .map(ClasspathBuilder::wildcardToPattern)
                .toList();

        List<Path> filteredEntries = entries.stream()
                .filter(path -> !matchesAny(path.getFileName() != null ? path.getFileName().toString() : "", excludePatterns))
                .distinct()
                .toList();

        String classpathString = filteredEntries.stream()
                .map(Path::toString)
                .collect(Collectors.joining(System.getProperty("path.separator")));
        return new ClasspathDescriptor(normalizedRoot, classesDir, filteredEntries, classpathString);
    }

    private List<Path> readClasspathEntries(Path classpathFile) {
        if (!Files.exists(classpathFile)) {
            log.warn("Classpath file {} does not exist; dependency jars will be skipped.", classpathFile);
            return List.of();
        }
        try {
            String raw = Files.readString(classpathFile, StandardCharsets.UTF_8).trim();
            if (raw.isEmpty()) {
                return List.of();
            }
            String[] parts = raw.split(System.getProperty("path.separator"));
            List<Path> resolved = new ArrayList<>();
            for (String part : parts) {
                if (part == null || part.isBlank()) {
                    continue;
                }
                resolved.add(Path.of(part.trim()));
            }
            return resolved;
        } catch (IOException ex) {
            log.warn("Failed to read {}: {}", classpathFile, ex.getMessage());
            return List.of();
        }
    }

    private void runMaven(Path workingDir, List<String> command) {
        Duration timeout = Duration.ofSeconds(Math.max(30, properties.getSafety().getMaxRuntimeSeconds()));
        commandRunner.run(workingDir, command, timeout, properties.getSafety().getMaxHeapMb());
    }

    private static boolean matchesAny(String candidate, List<Pattern> patterns) {
        if (candidate == null || candidate.isBlank()) {
            return false;
        }
        for (Pattern pattern : patterns) {
            if (pattern.matcher(candidate.toLowerCase(Locale.ROOT)).matches()) {
                return true;
            }
        }
        return false;
    }

    private static Pattern wildcardToPattern(String wildcard) {
        if (wildcard == null || wildcard.isBlank()) {
            return Pattern.compile("a^");
        }
        String normalized = wildcard.toLowerCase(Locale.ROOT)
                .replace(".", "\\.")
                .replace("*", ".*");
        return Pattern.compile(normalized);
    }

    public static final class ClasspathDescriptor {
        private final Path repoRoot;
        private final Path classesDirectory;
        private final List<Path> classpathEntries;
        private final String classpathString;

        public ClasspathDescriptor(Path repoRoot, Path classesDirectory, List<Path> classpathEntries, String classpathString) {
            this.repoRoot = repoRoot;
            this.classesDirectory = classesDirectory;
            this.classpathEntries = Collections.unmodifiableList(new ArrayList<>(classpathEntries));
            this.classpathString = classpathString;
        }

        public Path getRepoRoot() {
            return repoRoot;
        }

        public Path getClassesDirectory() {
            return classesDirectory;
        }

        public List<Path> getClasspathEntries() {
            return classpathEntries;
        }

        public String getClasspathString() {
            return classpathString;
        }
    }
}
