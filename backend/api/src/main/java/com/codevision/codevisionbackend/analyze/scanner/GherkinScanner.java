package com.codevision.codevisionbackend.analyze.scanner;

import com.codevision.codevisionbackend.analyze.GherkinFeatureSummary;
import com.codevision.codevisionbackend.analyze.GherkinScenarioSummary;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class GherkinScanner {

    private static final Logger log = LoggerFactory.getLogger(GherkinScanner.class);
    private static final Set<String> IGNORED_DIRECTORIES =
            Set.of(".git", "target", "build", "node_modules", ".idea", ".gradle", ".github");
    private static final List<String> STEP_PREFIXES = List.of("given", "when", "then", "and", "but");

    public List<GherkinFeatureSummary> scan(Path repoRoot) {
        if (repoRoot == null || !Files.exists(repoRoot)) {
            return List.of();
        }
        List<GherkinFeatureSummary> features = new ArrayList<>();
        FileVisitor<Path> visitor = new SimpleFileVisitor<>() {

            private final Set<Path> visitedDirectories = new HashSet<>();

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                Path dirName = dir.getFileName();
                if (dirName != null && IGNORED_DIRECTORIES.contains(dirName.toString())) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                Path normalized = dir.toAbsolutePath().normalize();
                if (!visitedDirectories.add(normalized)) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (!attrs.isRegularFile()) {
                    return FileVisitResult.CONTINUE;
                }
                String fileName = file.getFileName().toString().toLowerCase(Locale.ROOT);
                if (!fileName.endsWith(".feature")) {
                    return FileVisitResult.CONTINUE;
                }
                parseFeature(repoRoot, file).ifPresent(features::add);
                return FileVisitResult.CONTINUE;
            }
        };

        try {
            Files.walkFileTree(repoRoot, visitor);
        } catch (IOException e) {
            log.warn("Failed walking feature files under {}: {}", repoRoot, e.getMessage());
        }
        return features;
    }

    private Optional<GherkinFeatureSummary> parseFeature(Path repoRoot, Path file) {
        List<String> lines;
        try {
            lines = Files.readAllLines(file);
        } catch (IOException e) {
            log.debug("Failed reading feature file {}: {}", file, e.getMessage());
            return Optional.empty();
        }

        String featureTitle = null;
        List<GherkinScenarioSummary> scenarios = new ArrayList<>();
        ScenarioBuilder currentScenario = null;

        for (String rawLine : lines) {
            String line = rawLine.strip();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            String lowercase = line.toLowerCase(Locale.ROOT);
            if (lowercase.startsWith("feature:")) {
                featureTitle = line.substring(line.indexOf(':') + 1).trim();
                continue;
            }
            if (lowercase.startsWith("background:")) {
                currentScenario = finalizeScenario(currentScenario, scenarios);
                currentScenario = new ScenarioBuilder("Background", "BACKGROUND");
                continue;
            }
            if (lowercase.startsWith("scenario outline:")) {
                currentScenario = finalizeScenario(currentScenario, scenarios);
                currentScenario = new ScenarioBuilder(extractName(line), "SCENARIO_OUTLINE");
                continue;
            }
            if (lowercase.startsWith("scenario:")) {
                currentScenario = finalizeScenario(currentScenario, scenarios);
                currentScenario = new ScenarioBuilder(extractName(line), "SCENARIO");
                continue;
            }
            if (lowercase.startsWith("examples:")) {
                if (currentScenario == null) {
                    currentScenario = new ScenarioBuilder("Examples", "EXAMPLES");
                }
                currentScenario.addStep(line);
                continue;
            }
            if (isStepLine(lowercase) || line.startsWith("*") || line.startsWith("|")) {
                if (currentScenario == null) {
                    currentScenario = new ScenarioBuilder("Scenario", "SCENARIO");
                }
                currentScenario.addStep(line);
                continue;
            }
            if (currentScenario != null && (line.startsWith("\"\"\"") || line.startsWith(":"))) {
                currentScenario.addStep(line);
            }
        }
        finalizeScenario(currentScenario, scenarios);

        if (scenarios.isEmpty() && (featureTitle == null || featureTitle.isBlank())) {
            return Optional.empty();
        }

        String resolvedTitle = (featureTitle == null || featureTitle.isBlank())
                ? file.getFileName().toString().replace(".feature", "")
                : featureTitle;
        String relativePath = relativize(repoRoot, file);
        return Optional.of(new GherkinFeatureSummary(relativePath, resolvedTitle, scenarios));
    }

    private ScenarioBuilder finalizeScenario(ScenarioBuilder current, List<GherkinScenarioSummary> scenarios) {
        if (current != null && current.hasContent()) {
            scenarios.add(current.toSummary());
        }
        return null;
    }

    private boolean isStepLine(String lowercaseLine) {
        for (String keyword : STEP_PREFIXES) {
            if (lowercaseLine.startsWith(keyword + " ") || lowercaseLine.startsWith(keyword + "\t")) {
                return true;
            }
        }
        return false;
    }

    private String relativize(Path repoRoot, Path file) {
        try {
            Path normalizedRoot = repoRoot.toAbsolutePath().normalize();
            Path normalizedFile = file.toAbsolutePath().normalize();
            return normalizedRoot.relativize(normalizedFile).toString();
        } catch (IllegalArgumentException ex) {
            return file.toAbsolutePath().normalize().toString();
        }
    }

    private String extractName(String line) {
        int index = line.indexOf(':');
        if (index < 0) {
            return line.strip();
        }
        String name = line.substring(index + 1).trim();
        return name.isEmpty() ? "Scenario" : name;
    }

    private static final class ScenarioBuilder {

        private final String name;
        private final String type;
        private final List<String> steps = new ArrayList<>();

        private ScenarioBuilder(String name, String type) {
            this.name = name;
            this.type = type;
        }

        private void addStep(String line) {
            steps.add(line);
        }

        private boolean hasContent() {
            return (name != null && !name.isBlank()) || !steps.isEmpty();
        }

        private GherkinScenarioSummary toSummary() {
            return new GherkinScenarioSummary(name, type, List.copyOf(steps));
        }
    }
}
