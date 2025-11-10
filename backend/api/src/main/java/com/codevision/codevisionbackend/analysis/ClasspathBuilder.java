package com.codevision.codevisionbackend.analysis;

import com.codevision.codevisionbackend.analysis.config.CompiledAnalysisProperties;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.concurrent.TimeUnit;
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

    public ClasspathBuilder(CompiledAnalysisProperties properties) {
        this.properties = properties;
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
        Instant start = Instant.now();
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(workingDir.toFile());
        builder.redirectErrorStream(true);
        builder.environment().putIfAbsent("MAVEN_OPTS", "-Xmx" + properties.getSafety().getMaxHeapMb() + "m");

        try {
            Process process = builder.start();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.debug("[mvn] {}", line);
                }
            }
            boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new IllegalStateException("Maven command timed out after " + timeout.getSeconds() + " seconds");
            }
            int exitCode = process.exitValue();
            if (exitCode != 0) {
                throw new IllegalStateException("Maven command failed with exit code " + exitCode);
            }
            log.info("Command {} completed in {} ms", command, Duration.between(start, Instant.now()).toMillis());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Maven command interrupted: " + ex.getMessage(), ex);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed running command " + command + ": " + ex.getMessage(), ex);
        }
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
