package com.codevision.codevisionbackend.analyze.scanner;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseProblemException;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoggerScanner {

    private static final Logger log = LoggerFactory.getLogger(LoggerScanner.class);
    private static final Set<String> LOG_METHODS = Set.of("trace", "debug", "info", "warn", "error");
    private static final Set<String> LOGGER_IDENTIFIERS = Set.of("log", "logger");

    private final JavaParser javaParser;
    private final PiiPciInspector piiPciInspector;

    public LoggerScanner(PiiPciInspector piiPciInspector) {
        ParserConfiguration configuration = new ParserConfiguration()
                .setLanguageLevel(ParserConfiguration.LanguageLevel.BLEEDING_EDGE);
        this.javaParser = new JavaParser(configuration);
        this.piiPciInspector = piiPciInspector;
    }

    public List<LogStatementRecord> scan(Path repoRoot, List<Path> moduleRoots) {
        List<LogStatementRecord> records = new ArrayList<>();
        if (repoRoot == null || moduleRoots == null || moduleRoots.isEmpty()) {
            return List.of();
        }

        Path normalizedRoot = repoRoot.toAbsolutePath().normalize();
        Set<Path> visitedModules = new HashSet<>();

        for (Path moduleRoot : moduleRoots) {
            Path normalizedModule = moduleRoot.toAbsolutePath().normalize();
            if (!Files.isDirectory(normalizedModule) || !visitedModules.add(normalizedModule)) {
                continue;
            }
            scanSourceSet(normalizedRoot, normalizedModule.resolve("src/main/java"), records);
            scanSourceSet(normalizedRoot, normalizedModule.resolve("src/test/java"), records);
        }

        return List.copyOf(records);
    }

    private void scanSourceSet(Path repoRoot, Path sourceRoot, List<LogStatementRecord> collector) {
        if (!Files.isDirectory(sourceRoot)) {
            return;
        }

        try (Stream<Path> paths = Files.walk(sourceRoot, Integer.MAX_VALUE, FileVisitOption.FOLLOW_LINKS)) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .forEach(path -> parseFile(repoRoot, path, collector));
        } catch (IOException e) {
            log.warn("Failed to traverse source set {}: {}", sourceRoot, e.getMessage());
        }
    }

    private void parseFile(Path repoRoot, Path sourceFile, List<LogStatementRecord> collector) {
        try {
            Optional<CompilationUnit> unit = javaParser.parse(sourceFile).getResult();
            if (unit.isEmpty()) {
                return;
            }
            processCompilationUnit(repoRoot, sourceFile, unit.get(), collector);
        } catch (IOException e) {
            log.warn("Failed to read {}: {}", sourceFile, e.getMessage());
        } catch (ParseProblemException e) {
            log.debug("Skipping unparsable file {}: {}", sourceFile, e.getMessage());
        }
    }

    private void processCompilationUnit(
            Path repoRoot, Path sourceFile, CompilationUnit unit, List<LogStatementRecord> collector) {
        String packageName = unit.getPackageDeclaration()
                .map(pkg -> pkg.getName().asString())
                .orElse("");
        String relativePath = deriveRelativePath(repoRoot, sourceFile);
        for (TypeDeclaration<?> type : unit.getTypes()) {
            String className = resolveClassName(packageName, type);
            type.findAll(MethodCallExpr.class)
                    .forEach(call -> handleMethodCall(className, relativePath, call, collector));
        }
    }

    private void handleMethodCall(
            String className,
            String relativePath,
            MethodCallExpr call,
            List<LogStatementRecord> collector) {
        String methodName = call.getNameAsString();
        if (!LOG_METHODS.contains(methodName.toLowerCase(Locale.ROOT))) {
            return;
        }
        if (!isLoggerInvocation(call.getScope())) {
            return;
        }
        String message = extractMessageTemplate(call);
        List<String> variables = extractVariables(call);
        PiiPciInspector.RiskAssessment messageRisk = piiPciInspector.assessText(message);
        PiiPciInspector.RiskAssessment variablesRisk = variables.stream()
                .map(piiPciInspector::assessText)
                .reduce(PiiPciInspector.RiskAssessment.none(), PiiPciInspector.RiskAssessment::combine);
        PiiPciInspector.RiskAssessment combined = messageRisk.combine(variablesRisk);
        int lineNumber = call.getRange().map(range -> range.begin.line).orElse(-1);
        collector.add(new LogStatementRecord(
                className,
                relativePath,
                methodName.toUpperCase(Locale.ROOT),
                lineNumber,
                message,
                variables,
                combined.piiRisk(),
                combined.pciRisk()));
    }

    private String deriveRelativePath(Path repoRoot, Path sourceFile) {
        if (repoRoot == null || sourceFile == null) {
            return sourceFile != null ? sourceFile.getFileName().toString() : "";
        }
        Path normalizedRoot = repoRoot.toAbsolutePath().normalize();
        Path normalizedFile = sourceFile.toAbsolutePath().normalize();
        try {
            return normalizedRoot.relativize(normalizedFile).toString();
        } catch (IllegalArgumentException ex) {
            return normalizedFile.getFileName().toString();
        }
    }

    private boolean isLoggerInvocation(Optional<Expression> scope) {
        if (scope.isEmpty()) {
            return false;
        }
        String candidate = scope.get().toString().toLowerCase(Locale.ROOT);
        for (String identifier : LOGGER_IDENTIFIERS) {
            if (candidate.contains(identifier)) {
                return true;
            }
        }
        return false;
    }

    private String extractMessageTemplate(MethodCallExpr call) {
        if (call.getArguments().isEmpty()) {
            return "";
        }
        Expression first = call.getArgument(0);
        if (first instanceof StringLiteralExpr literal) {
            return literal.getValue();
        }
        return first.toString();
    }

    private List<String> extractVariables(MethodCallExpr call) {
        if (call.getArguments().size() <= 1) {
            return List.of();
        }
        List<String> variables = new ArrayList<>();
        for (int i = 1; i < call.getArguments().size(); i++) {
            variables.add(call.getArgument(i).toString());
        }
        return variables;
    }

    private String resolveClassName(String packageName, TypeDeclaration<?> type) {
        return type.getFullyQualifiedName()
                .orElseGet(() -> composeFqn(packageName, type.getNameAsString()));
    }

    private String composeFqn(String packageName, String simpleName) {
        if (packageName == null || packageName.isBlank()) {
            return simpleName;
        }
        return packageName + "." + simpleName;
    }
}
