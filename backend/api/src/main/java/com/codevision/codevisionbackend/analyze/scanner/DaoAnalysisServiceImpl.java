package com.codevision.codevisionbackend.analyze.scanner;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseProblemException;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DaoAnalysisServiceImpl implements DaoAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(DaoAnalysisServiceImpl.class);
    private static final Set<String> SPRING_DATA_BASE_INTERFACES = Set.of(
            "jparepository",
            "crudrepository",
            "pagingandsortingrepository",
            "repository");
    private static final Set<String> QUERY_ANNOTATIONS = Set.of("query");
    private static final int MAX_WALK_DEPTH = Integer.MAX_VALUE;

    private final JavaParser javaParser;

    public DaoAnalysisServiceImpl() {
        ParserConfiguration configuration = new ParserConfiguration()
                .setLanguageLevel(ParserConfiguration.LanguageLevel.BLEEDING_EDGE);
        this.javaParser = new JavaParser(configuration);
    }

    @Override
    public DbAnalysisResult analyze(Path repoRoot, List<Path> moduleRoots, List<DbEntityRecord> entities) {
        if (moduleRoots == null || moduleRoots.isEmpty()) {
            return new DbAnalysisResult(List.of(), Map.of(), Map.of());
        }

        Map<String, DbEntityRecord> entityBySimpleName = entities == null
                ? Map.of()
                : entities.stream()
                        .collect(Collectors.toMap(
                                DbEntityRecord::className,
                                record -> record,
                                (left, right) -> left,
                                HashMap::new));

        Map<String, Set<String>> classesByEntity = new HashMap<>();
        Map<String, List<DaoOperationRecord>> operationsByClass = new HashMap<>();
        Set<Path> visitedSourceRoots = new HashSet<>();

        for (Path moduleRoot : moduleRoots) {
            Path sourceRoot = moduleRoot.resolve("src/main/java").toAbsolutePath().normalize();
            if (!Files.isDirectory(sourceRoot) || !visitedSourceRoots.add(sourceRoot)) {
                continue;
            }
            traverseSourceRoot(sourceRoot, entityBySimpleName, classesByEntity, operationsByClass);
        }

        if (!entityBySimpleName.isEmpty()) {
            for (DbEntityRecord entity : entityBySimpleName.values()) {
                classesByEntity.computeIfAbsent(entity.className(), key -> new HashSet<>());
            }
        }

        Map<String, List<String>> classesByEntityView = classesByEntity.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().stream().sorted().toList()));

        Map<String, List<DaoOperationRecord>> operationsView = operationsByClass.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().stream()
                                .sorted((left, right) -> left.methodName().compareToIgnoreCase(right.methodName()))
                                .toList()));

        return new DbAnalysisResult(entities == null ? List.of() : entities, classesByEntityView, operationsView);
    }

    private void traverseSourceRoot(
            Path sourceRoot,
            Map<String, DbEntityRecord> entities,
            Map<String, Set<String>> classesByEntity,
            Map<String, List<DaoOperationRecord>> operationsByClass) {
        try (Stream<Path> paths = Files.walk(sourceRoot, MAX_WALK_DEPTH, FileVisitOption.FOLLOW_LINKS)) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .forEach(path -> parseSource(path, entities, classesByEntity, operationsByClass));
        } catch (IOException e) {
            log.warn("Failed traversing {} for DAO analysis: {}", sourceRoot, e.getMessage());
        }
    }

    private void parseSource(
            Path sourceFile,
            Map<String, DbEntityRecord> entities,
            Map<String, Set<String>> classesByEntity,
            Map<String, List<DaoOperationRecord>> operationsByClass) {
        try {
            Optional<CompilationUnit> unitOpt = javaParser.parse(sourceFile).getResult();
            if (unitOpt.isEmpty()) {
                return;
            }
            CompilationUnit unit = unitOpt.get();
            String packageName = unit.getPackageDeclaration()
                    .map(pkg -> pkg.getName().asString())
                    .orElse("");

            for (TypeDeclaration<?> declaration : unit.getTypes()) {
                if (!(declaration instanceof ClassOrInterfaceDeclaration classDeclaration)) {
                    continue;
                }
                if (!classDeclaration.isInterface()) {
                    continue;
                }
                processRepositoryInterface(classDeclaration, packageName, entities, classesByEntity, operationsByClass);
            }
        } catch (IOException | ParseProblemException ex) {
            log.debug("Failed parsing {} for repository metadata: {}", sourceFile, ex.getMessage());
        }
    }

    private void processRepositoryInterface(
            ClassOrInterfaceDeclaration declaration,
            String packageName,
            Map<String, DbEntityRecord> entities,
            Map<String, Set<String>> classesByEntity,
            Map<String, List<DaoOperationRecord>> operationsByClass) {
        Optional<ClassOrInterfaceType> repositoryBase = declaration.getExtendedTypes().stream()
                .filter(type -> isRepositoryBase(type.getName().getIdentifier()))
                .findFirst();
        if (repositoryBase.isEmpty()) {
            return;
        }

        String repositoryName = declaration.getNameAsString();
        String repositoryFqn = declaration.getFullyQualifiedName()
                .orElseGet(() -> composeFqn(packageName, repositoryName));

        String entityType = resolveEntityType(repositoryBase.get());
        if (entityType == null) {
            log.debug("Unable to resolve entity type for repository {}", repositoryFqn);
            return;
        }
        String entitySimpleName = simpleName(entityType);
        DbEntityRecord entityRecord = entities.get(entitySimpleName);

        String entityKey = entityRecord != null ? entityRecord.className() : entitySimpleName;
        classesByEntity
                .computeIfAbsent(entityKey, key -> new HashSet<>())
                .add(repositoryFqn);

        List<DaoOperationRecord> operations = collectOperations(declaration, repositoryFqn, entityRecord, entitySimpleName);
        if (!operations.isEmpty()) {
            operationsByClass
                    .computeIfAbsent(repositoryFqn, key -> new ArrayList<>())
                    .addAll(operations);
        }
    }

    private List<DaoOperationRecord> collectOperations(
            ClassOrInterfaceDeclaration declaration,
            String repositoryFqn,
            DbEntityRecord entityRecord,
            String entityFallback) {
        List<DaoOperationRecord> operations = new ArrayList<>();
        String targetDescriptor = buildTargetDescriptor(entityRecord, entityFallback);

        for (MethodDeclaration method : declaration.getMethods()) {
            if (method.isDefault() || method.isStatic()) {
                continue;
            }
            Optional<String> query = extractQuery(method);
            String methodName = method.getNameAsString();
            String operationType = inferOperationType(methodName, query);
            String querySnippet = query.orElse(null);
            operations.add(new DaoOperationRecord(repositoryFqn, methodName, operationType, targetDescriptor, querySnippet));
        }
        return operations;
    }

    private String buildTargetDescriptor(DbEntityRecord entityRecord, String fallback) {
        if (entityRecord == null) {
            return fallback;
        }
        String tableName = entityRecord.tableName();
        if (tableName == null || tableName.isBlank() || tableName.equals(entityRecord.className())) {
            return entityRecord.className();
        }
        return entityRecord.className() + " [" + tableName + "]";
    }

    private Optional<String> extractQuery(MethodDeclaration method) {
        return method.getAnnotations().stream()
                .filter(annotation -> QUERY_ANNOTATIONS.contains(annotation.getName().getIdentifier().toLowerCase(Locale.ROOT)))
                .map(annotation -> {
                    if (annotation instanceof SingleMemberAnnotationExpr singleMember) {
                        return stripQuotes(singleMember.getMemberValue().toString());
                    }
                    if (annotation instanceof NormalAnnotationExpr normalAnnotation) {
                        for (MemberValuePair pair : normalAnnotation.getPairs()) {
                            if (pair.getName().asString().equalsIgnoreCase("value")) {
                                return stripQuotes(pair.getValue().toString());
                            }
                        }
                    }
                    return annotation.toString();
                })
                .findFirst();
    }

    private String stripQuotes(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if ((trimmed.startsWith("\"") && trimmed.endsWith("\""))
                || (trimmed.startsWith("'") && trimmed.endsWith("'"))) {
            return trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed;
    }

    private String inferOperationType(String methodName, Optional<String> query) {
        if (query.isPresent()) {
            String normalized = query.get().trim().toLowerCase(Locale.ROOT);
            if (normalized.startsWith("select")) {
                return "SELECT";
            }
            if (normalized.startsWith("insert")) {
                return "INSERT";
            }
            if (normalized.startsWith("update")) {
                return "UPDATE";
            }
            if (normalized.startsWith("delete") || normalized.startsWith("remove")) {
                return "DELETE";
            }
        }

        String name = methodName.toLowerCase(Locale.ROOT);
        if (startsWithAny(name, "find", "get", "read", "query", "count", "exists", "stream")) {
            return "SELECT";
        }
        if (startsWithAny(name, "save", "insert", "create", "add", "persist")) {
            return "INSERT_OR_UPDATE";
        }
        if (startsWithAny(name, "update", "set")) {
            return "UPDATE";
        }
        if (startsWithAny(name, "delete", "remove", "purge", "truncate")) {
            return "DELETE";
        }
        return "UNKNOWN";
    }

    private boolean startsWithAny(String value, String... prefixes) {
        for (String prefix : prefixes) {
            if (value.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private boolean isRepositoryBase(String simpleName) {
        if (simpleName == null) {
            return false;
        }
        String normalized = simpleName.toLowerCase(Locale.ROOT);
        if (SPRING_DATA_BASE_INTERFACES.contains(normalized)) {
            return true;
        }
        return normalized.endsWith("repository");
    }

    private String resolveEntityType(ClassOrInterfaceType repositoryType) {
        return repositoryType.getTypeArguments()
                .flatMap(args -> args.isEmpty() ? Optional.empty() : Optional.of(args.get(0).asString()))
                .orElse(null);
    }

    private String simpleName(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        int lastDot = value.lastIndexOf('.');
        if (lastDot >= 0 && lastDot + 1 < value.length()) {
            return value.substring(lastDot + 1);
        }
        return value;
    }

    private String composeFqn(String packageName, String simpleName) {
        if (packageName == null || packageName.isBlank()) {
            return simpleName;
        }
        return packageName + "." + simpleName;
    }
}
