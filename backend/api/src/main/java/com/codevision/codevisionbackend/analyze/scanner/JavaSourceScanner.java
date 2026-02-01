package com.codevision.codevisionbackend.analyze.scanner;

import com.codevision.codevisionbackend.analyze.scanner.ClassMetadataRecord.SourceSet;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseProblemException;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class JavaSourceScanner {

    private static final Logger log = LoggerFactory.getLogger(JavaSourceScanner.class);
    private static final Set<String> CONTROLLER_ANNOTATIONS = Set.of(
            "RestController",
            "Controller",
            "GraphQLController");
    private static final Set<String> SERVICE_ANNOTATIONS = Set.of(
            "Service",
            "Component");
    private static final Set<String> REPOSITORY_ANNOTATIONS = Set.of(
            "Repository",
            "JpaRepository");
    private static final Set<String> ENTITY_ANNOTATIONS = Set.of("Entity", "Document");
    private static final Set<String> CONFIG_ANNOTATIONS = Set.of("Configuration");

    private static final List<String> USER_CODE_PREFIXES = List.of("com.barclays", "com.codeviz2");
    private final JavaParser javaParser;

    public JavaSourceScanner() {
        ParserConfiguration configuration = new ParserConfiguration()
                .setLanguageLevel(ParserConfiguration.LanguageLevel.BLEEDING_EDGE);
        this.javaParser = new JavaParser(configuration);
    }

    public List<ClassMetadataRecord> scan(Path repoRoot, List<Path> moduleRoots) {
        List<ClassMetadataRecord> records = new ArrayList<>();
        Path normalizedRoot = repoRoot.toAbsolutePath().normalize();
        Set<Path> visitedRoots = new HashSet<>();

        for (Path moduleRoot : moduleRoots) {
            Path normalizedModule = moduleRoot.toAbsolutePath().normalize();
            if (!Files.isDirectory(normalizedModule) || !visitedRoots.add(normalizedModule)) {
                continue;
            }
            collectFromSourceSet(normalizedRoot, normalizedModule.resolve("src/main/java"), SourceSet.MAIN, records);
        }

        return records;
    }

    private void collectFromSourceSet(
            Path repoRoot, Path sourceRoot, SourceSet sourceSet, List<ClassMetadataRecord> collector) {
        if (!Files.isDirectory(sourceRoot)) {
            return;
        }

        try (Stream<Path> paths = Files.walk(sourceRoot, Integer.MAX_VALUE, FileVisitOption.FOLLOW_LINKS)) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> !AnalysisExclusions.isExcludedPath(path))
                    .forEach(path -> parseJavaFile(repoRoot, sourceSet, path, collector));
        } catch (IOException e) {
            log.warn("Failed traversing source set {}: {}", sourceRoot, e.getMessage());
        }
    }

    private void parseJavaFile(
            Path repoRoot, SourceSet sourceSet, Path sourceFile, List<ClassMetadataRecord> collector) {
        try {
            Optional<CompilationUnit> compilationUnit = javaParser.parse(sourceFile).getResult();
            if (compilationUnit.isEmpty()) {
                return;
            }
            processCompilationUnit(repoRoot, sourceSet, sourceFile, compilationUnit.get(), collector);
        } catch (IOException e) {
            log.warn("Failed to read {}: {}", sourceFile, e.getMessage());
        } catch (ParseProblemException e) {
            log.warn("Failed to parse {}: {}", sourceFile, e.getMessage());
        }
    }

    private void processCompilationUnit(
            Path repoRoot,
            SourceSet sourceSet,
            Path sourceFile,
            CompilationUnit unit,
            List<ClassMetadataRecord> collector) {
        String packageName = unit.getPackageDeclaration()
                .map(pkg -> pkg.getName().asString())
                .orElse("");
        for (TypeDeclaration<?> type : unit.getTypes()) {
            if (type.isAnnotationDeclaration()) {
                continue;
            }
            String fullyQualifiedName = type.getFullyQualifiedName()
                    .orElseGet(() -> composeFqn(packageName, type.getNameAsString()));
            boolean userCode = isUserCodePackage(fullyQualifiedName);

            List<String> annotations = extractAnnotations(type);
            List<String> implementedInterfaces = extractImplementedInterfaces(type);
            String stereotype = determineStereotype(type, annotations, sourceSet);
            String relativePath = deriveRelativePath(repoRoot, sourceFile);
            if (AnalysisExclusions.isExcludedPath(relativePath)
                    || AnalysisExclusions.isMockClassName(type.getNameAsString())) {
                continue;
            }

            collector.add(new ClassMetadataRecord(
                    fullyQualifiedName,
                    packageName,
                    type.getNameAsString(),
                    annotations,
                    implementedInterfaces,
                    stereotype,
                    sourceSet,
                    relativePath,
                    userCode));
        }
    }

    private String deriveRelativePath(Path repoRoot, Path sourceFile) {
        Path normalizedRoot = repoRoot.toAbsolutePath().normalize();
        Path normalizedFile = sourceFile.toAbsolutePath().normalize();
        try {
            return normalizedRoot.relativize(normalizedFile).toString();
        } catch (IllegalArgumentException e) {
            return sourceFile.getFileName().toString();
        }
    }

    private List<String> extractAnnotations(NodeWithAnnotations<?> type) {
        return type.getAnnotations().stream()
                .map(annotation -> annotation.getName().getIdentifier())
                .distinct()
                .toList();
    }

    private List<String> extractImplementedInterfaces(TypeDeclaration<?> type) {
        if (type instanceof ClassOrInterfaceDeclaration declaration) {
            Stream<String> implemented = declaration.getImplementedTypes().stream()
                    .map(ClassOrInterfaceType::getNameAsString);
            Stream<String> extended = declaration.getExtendedTypes().stream()
                    .map(ClassOrInterfaceType::getNameAsString);
            return Stream.concat(implemented, extended)
                    .distinct()
                    .collect(Collectors.toList());
        }
        if (type instanceof RecordDeclaration recordDeclaration) {
            return recordDeclaration.getImplementedTypes().stream()
                    .map(ClassOrInterfaceType::getNameAsString)
                    .collect(Collectors.toList());
        }
        if (type instanceof EnumDeclaration enumDeclaration) {
            return enumDeclaration.getImplementedTypes().stream()
                    .map(ClassOrInterfaceType::getNameAsString)
                    .collect(Collectors.toList());
        }
        return List.of();
    }

    private String determineStereotype(
            TypeDeclaration<?> type, List<String> annotations, SourceSet sourceSet) {
        boolean isTestSource = sourceSet == SourceSet.TEST;
        String simpleName = type.getNameAsString();
        Set<String> annotationNames = annotations.stream()
                .map(name -> name.toLowerCase(Locale.ROOT))
                .collect(Collectors.toCollection(HashSet::new));
        if (isTestSource
                || simpleName.endsWith("Test")
                || annotationNames.contains("test")) {
            return "TEST";
        }
        if (hasAnnotation(annotations, CONTROLLER_ANNOTATIONS)
                || simpleName.endsWith("Controller")) {
            return "CONTROLLER";
        }
        if (hasAnnotation(annotations, SERVICE_ANNOTATIONS)
                || simpleName.endsWith("Service")) {
            return "SERVICE";
        }
        if (hasAnnotation(annotations, REPOSITORY_ANNOTATIONS)
                || simpleName.endsWith("Repository")
                || simpleName.endsWith("Dao")) {
            return "REPOSITORY";
        }
        if (hasAnnotation(annotations, ENTITY_ANNOTATIONS)) {
            return "ENTITY";
        }
        if (hasAnnotation(annotations, CONFIG_ANNOTATIONS)
                || simpleName.endsWith("Config")
                || simpleName.endsWith("Configuration")) {
            return "CONFIG";
        }
        if (type instanceof EnumDeclaration || simpleName.endsWith("Util")) {
            return "UTILITY";
        }
        if (type instanceof RecordDeclaration) {
            return "ENTITY";
        }
        return "OTHER";
    }

    private boolean hasAnnotation(List<String> annotations, Set<String> targets) {
        for (String annotation : annotations) {
            String normalized = annotation.toLowerCase(Locale.ROOT);
            for (String target : targets) {
                if (normalized.endsWith(target.toLowerCase(Locale.ROOT))) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isUserCodePackage(String fullyQualifiedName) {
        if (fullyQualifiedName == null || fullyQualifiedName.isBlank()) {
            return false;
        }
        String normalized = fullyQualifiedName.trim();
        if (USER_CODE_PREFIXES.isEmpty()) {
            return true;
        }
        for (String prefix : USER_CODE_PREFIXES) {
            if (normalized.equals(prefix)) {
                return true;
            }
            if (normalized.startsWith(prefix + ".") || normalized.startsWith(prefix + "$")) {
                return true;
            }
        }
        return false;
    }

    private String composeFqn(String packageName, String simpleName) {
        if (packageName == null || packageName.isBlank()) {
            return simpleName;
        }
        return packageName + "." + simpleName;
    }
}
