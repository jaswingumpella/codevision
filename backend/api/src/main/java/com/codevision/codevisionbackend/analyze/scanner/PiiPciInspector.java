package com.codevision.codevisionbackend.analyze.scanner;

import com.codevision.codevisionbackend.config.SecurityScanProperties;
import com.codevision.codevisionbackend.config.SecurityScanProperties.Rule;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class PiiPciInspector {

    private static final Logger log = LoggerFactory.getLogger(PiiPciInspector.class);
    private static final Set<String> TEXT_FILE_EXTENSIONS = Set.of(
            ".java",
            ".yml",
            ".yaml",
            ".xml",
            ".properties",
            ".sql",
            ".log",
            ".wsdl",
            ".xsd",
            ".feature",
            ".txt",
            ".json",
            ".csv",
            ".md");

    private final List<CompiledRule> rules;
    private final List<Pattern> ignorePatterns;

    public PiiPciInspector(SecurityScanProperties properties) {
        this.rules = compileRules(properties.getRules());
        this.ignorePatterns = compileIgnorePatterns(properties.getIgnorePatterns());
    }

    public List<PiiPciFindingRecord> scan(Path repoRoot) {
        return scan(repoRoot, List.of(repoRoot));
    }

    public List<PiiPciFindingRecord> scan(Path repoRoot, List<Path> includeRoots) {
        if (rules.isEmpty() || repoRoot == null) {
            return List.of();
        }
        Path normalizedRoot = repoRoot.toAbsolutePath().normalize();
        List<Path> targets = includeRoots == null || includeRoots.isEmpty() ? List.of(normalizedRoot) : includeRoots;
        List<PiiPciFindingRecord> findings = new ArrayList<>();
        Set<Path> visited = new HashSet<>();
        for (Path target : targets) {
            if (target == null) {
                continue;
            }
            Path normalizedTarget = target.toAbsolutePath().normalize();
            if (!Files.exists(normalizedTarget) || !visited.add(normalizedTarget)) {
                continue;
            }
            walkTarget(normalizedRoot, normalizedTarget, findings);
        }
        return List.copyOf(findings);
    }

    private void walkTarget(Path repoRoot, Path targetRoot, List<PiiPciFindingRecord> findings) {
        try {
            Files.walkFileTree(targetRoot, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (!attrs.isRegularFile() || !isTextCandidate(file)) {
                        return FileVisitResult.CONTINUE;
                    }
                    scanFile(repoRoot, file, findings);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            log.warn("Failed to traverse {} for PII/PCI scanning: {}", targetRoot, e.getMessage());
        }
    }

    public RiskAssessment assessText(String text) {
        if (!StringUtils.hasText(text) || rules.isEmpty()) {
            return RiskAssessment.none();
        }
        String candidate = text;
        boolean pii = false;
        boolean pci = false;
        for (CompiledRule rule : rules) {
            if (rule.matches(candidate)) {
                if (rule.isPci()) {
                    pci = true;
                }
                if (rule.isPii()) {
                    pii = true;
                }
            }
        }
        return new RiskAssessment(pii, pci);
    }

    private void scanFile(Path repoRoot, Path file, List<PiiPciFindingRecord> findings) {
        Path normalizedFile = file.toAbsolutePath().normalize();
        String relativePath = relativize(repoRoot, normalizedFile);

        try (BufferedReader reader = Files.newBufferedReader(normalizedFile, StandardCharsets.UTF_8)) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                evaluateLine(relativePath, line, lineNumber, findings);
            }
        } catch (IOException e) {
            log.warn("Failed to inspect {}: {}", relativePath, e.getMessage());
        }
    }

    private void evaluateLine(
            String relativePath, String line, int lineNumber, List<PiiPciFindingRecord> findings) {
        if (!StringUtils.hasText(line)) {
            return;
        }
        String candidate = line;
        for (CompiledRule rule : rules) {
            if (!rule.matches(candidate)) {
                continue;
            }
            boolean ignored = isIgnored(relativePath, candidate);
            String snippet = truncateSnippet(candidate);
            findings.add(new PiiPciFindingRecord(
                    relativePath, lineNumber, snippet, rule.type(), rule.severity(), ignored));
        }
    }

    private boolean isIgnored(String relativePath, String line) {
        if (ignorePatterns.isEmpty()) {
            return false;
        }
        for (Pattern pattern : ignorePatterns) {
            if ((relativePath != null && pattern.matcher(relativePath).find())
                    || (line != null && pattern.matcher(line).find())) {
                return true;
            }
        }
        return false;
    }

    private boolean isTextCandidate(Path file) {
        String fileName = file.getFileName().toString().toLowerCase(Locale.ROOT);
        for (String extension : TEXT_FILE_EXTENSIONS) {
            if (fileName.endsWith(extension)) {
                return true;
            }
        }
        return false;
    }

    private String relativize(Path root, Path file) {
        try {
            return root.relativize(file).toString();
        } catch (IllegalArgumentException ex) {
            return file.getFileName().toString();
        }
    }

    private List<CompiledRule> compileRules(List<Rule> configuredRules) {
        if (configuredRules == null || configuredRules.isEmpty()) {
            return List.of();
        }
        List<CompiledRule> compiled = new ArrayList<>();
        for (Rule rule : configuredRules) {
            if (rule == null) {
                continue;
            }
            String keyword = StringUtils.hasText(rule.getKeyword())
                    ? rule.getKeyword().toLowerCase(Locale.ROOT)
                    : null;
            Pattern pattern = StringUtils.hasText(rule.getRegex()) ? safeCompile(rule.getRegex()) : null;
            if (keyword == null && pattern == null) {
                continue;
            }
            String type = StringUtils.hasText(rule.getType())
                    ? rule.getType().toUpperCase(Locale.ROOT)
                    : "UNKNOWN";
            String severity = StringUtils.hasText(rule.getSeverity())
                    ? rule.getSeverity().toUpperCase(Locale.ROOT)
                    : "LOW";
            compiled.add(new CompiledRule(keyword, pattern, type, severity));
        }
        return List.copyOf(compiled);
    }

    private List<Pattern> compileIgnorePatterns(List<String> patterns) {
        if (patterns == null || patterns.isEmpty()) {
            return List.of();
        }
        Set<Pattern> compiled = new HashSet<>();
        for (String pattern : patterns) {
            if (!StringUtils.hasText(pattern)) {
                continue;
            }
            Pattern compiledPattern = safeCompile(pattern);
            if (compiledPattern != null) {
                compiled.add(compiledPattern);
            }
        }
        return List.copyOf(compiled);
    }

    private Pattern safeCompile(String expression) {
        try {
            return Pattern.compile(expression, Pattern.CASE_INSENSITIVE);
        } catch (Exception ex) {
            log.warn("Failed to compile regex '{}': {}", expression, ex.getMessage());
            return null;
        }
    }

    private String truncateSnippet(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        return trimmed.length() > 500 ? trimmed.substring(0, 500) : trimmed;
    }

    public record RiskAssessment(boolean piiRisk, boolean pciRisk) {

        private static final RiskAssessment NONE = new RiskAssessment(false, false);

        public static RiskAssessment none() {
            return NONE;
        }

        public RiskAssessment combine(RiskAssessment other) {
            if (other == null) {
                return this;
            }
            return new RiskAssessment(this.piiRisk || other.piiRisk, this.pciRisk || other.pciRisk);
        }
    }

    private static final class CompiledRule {

        private final String keyword;
        private final Pattern pattern;
        private final String type;
        private final String severity;

        private CompiledRule(String keyword, Pattern pattern, String type, String severity) {
            this.keyword = keyword;
            this.pattern = pattern;
            this.type = type;
            this.severity = severity;
        }

        private boolean matches(String candidate) {
            if (candidate == null) {
                return false;
            }
            String content = candidate;
            if (keyword != null && !content.toLowerCase(Locale.ROOT).contains(keyword)) {
                return false;
            }
            if (pattern != null) {
                return pattern.matcher(content).find();
            }
            return true;
        }

        private String type() {
            return type;
        }

        private String severity() {
            return severity;
        }

        private boolean isPii() {
            return "PII".equalsIgnoreCase(type);
        }

        private boolean isPci() {
            return "PCI".equalsIgnoreCase(type);
        }
    }
}
