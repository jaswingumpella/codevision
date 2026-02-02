package com.codevision.codevisionbackend.analyze.scanner;

import com.codevision.codevisionbackend.analysis.ClasspathBuilder;
import com.codevision.codevisionbackend.analysis.config.CompiledAnalysisProperties;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseProblemException;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
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
import org.springframework.beans.factory.annotation.Autowired;
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
    private static final Set<String> DAO_CLASS_ANNOTATIONS = Set.of("repository", "component");
    private static final Set<String> DAO_FIELD_TYPES = Set.of(
            "sessionfactory",
            "session",
            "entitymanager",
            "jdbctemplate",
            "namedparameterjdbctemplate",
            "hibernatetemplate");
    private static final Set<String> DAO_INSERT_UPDATE_METHODS =
            Set.of("save", "saveorupdate", "persist", "merge");
    private static final Set<String> DAO_UPDATE_METHODS =
            Set.of("update", "flush", "executeupdate");
    private static final Set<String> DAO_DELETE_METHODS = Set.of("delete", "remove");
    private static final Set<String> DAO_SELECT_METHODS = Set.of(
            "find",
            "get",
            "load",
            "list",
            "getresultlist",
            "getsingleresult",
            "uniqueresult",
            "createquery",
            "createnativequery",
            "createsqlquery",
            "createcriteria",
            "getcriteriabuilder",
            "createcriteriabuilder");
    private static final Set<String> DAO_SCOPE_HINTS = Set.of(
            "session",
            "entitymanager",
            "jdbctemplate",
            "jdbc",
            "criteria",
            "query",
            "hibernate");
    private static final int MAX_WALK_DEPTH = Integer.MAX_VALUE;

    private final JavaParser javaParser;
    private final ClasspathBuilder classpathBuilder;
    private final CompiledAnalysisProperties compiledAnalysisProperties;
    private final BytecodeDaoScanner bytecodeDaoScanner;

    public DaoAnalysisServiceImpl() {
        this(null, null, null);
    }

    @Autowired
    public DaoAnalysisServiceImpl(
            ClasspathBuilder classpathBuilder,
            CompiledAnalysisProperties compiledAnalysisProperties,
            BytecodeDaoScanner bytecodeDaoScanner) {
        ParserConfiguration configuration = new ParserConfiguration()
                .setLanguageLevel(ParserConfiguration.LanguageLevel.BLEEDING_EDGE);
        this.javaParser = new JavaParser(configuration);
        this.classpathBuilder = classpathBuilder;
        this.compiledAnalysisProperties = compiledAnalysisProperties;
        this.bytecodeDaoScanner = bytecodeDaoScanner;
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

        Map<String, RepositoryDescriptor> repositoryIndex = new HashMap<>();
        Map<String, Set<String>> usedRepositoryMethods = new HashMap<>();
        Map<String, Set<String>> classesByEntity = new HashMap<>();
        Map<String, List<DaoOperationRecord>> operationsByClass = new HashMap<>();
        Set<Path> visitedSourceRoots = new HashSet<>();

        for (Path moduleRoot : moduleRoots) {
            Path sourceRoot = moduleRoot.resolve("src/main/java").toAbsolutePath().normalize();
            if (!Files.isDirectory(sourceRoot) || !visitedSourceRoots.add(sourceRoot)) {
                continue;
            }
            indexRepositoryInterfaces(sourceRoot, entityBySimpleName, repositoryIndex);
        }

        visitedSourceRoots.clear();
        for (Path moduleRoot : moduleRoots) {
            Path sourceRoot = moduleRoot.resolve("src/main/java").toAbsolutePath().normalize();
            if (!Files.isDirectory(sourceRoot) || !visitedSourceRoots.add(sourceRoot)) {
                continue;
            }
            traverseSourceRoot(
                    sourceRoot,
                    entityBySimpleName,
                    repositoryIndex,
                    usedRepositoryMethods,
                    classesByEntity,
                    operationsByClass);
        }

        appendRepositoryOperations(repositoryIndex, usedRepositoryMethods, classesByEntity, operationsByClass);
        mergeBytecodeOperations(repoRoot, moduleRoots, entityBySimpleName, classesByEntity, operationsByClass);

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
            Map<String, RepositoryDescriptor> repositoryIndex,
            Map<String, Set<String>> usedRepositoryMethods,
            Map<String, Set<String>> classesByEntity,
            Map<String, List<DaoOperationRecord>> operationsByClass) {
        try (Stream<Path> paths = Files.walk(sourceRoot, MAX_WALK_DEPTH, FileVisitOption.FOLLOW_LINKS)) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> !AnalysisExclusions.isExcludedPath(path))
                    .forEach(path -> parseSource(
                            path,
                            entities,
                            repositoryIndex,
                            usedRepositoryMethods,
                            classesByEntity,
                            operationsByClass));
        } catch (IOException e) {
            log.warn("Failed traversing {} for DAO analysis: {}", sourceRoot, e.getMessage());
        }
    }

    private void mergeBytecodeOperations(
            Path repoRoot,
            List<Path> moduleRoots,
            Map<String, DbEntityRecord> entities,
            Map<String, Set<String>> classesByEntity,
            Map<String, List<DaoOperationRecord>> operationsByClass) {
        if (classpathBuilder == null || bytecodeDaoScanner == null || compiledAnalysisProperties == null) {
            return;
        }
        try {
            List<String> acceptPackages = compiledAnalysisProperties.getAcceptPackages();
            boolean includeDependencies = compiledAnalysisProperties.isIncludeDependencies();
            ClasspathBuilder.ClasspathDescriptor classpath =
                    classpathBuilder.buildForModules(repoRoot, moduleRoots, includeDependencies);
            if (classpath.getClasspathEntries().isEmpty()) {
                log.warn("Bytecode DAO scanning skipped because no classpath entries were resolved.");
                return;
            }
            Map<String, List<DaoOperationRecord>> bytecodeOps =
                    bytecodeDaoScanner.scan(classpath, acceptPackages, entities);
            if (bytecodeOps.isEmpty()) {
                return;
            }
            mergeOperations(operationsByClass, bytecodeOps);
            bytecodeOps.forEach((className, ops) -> {
                DbEntityRecord record = resolveEntityForDaoClassName(className, entities);
                if (record != null) {
                    classesByEntity
                            .computeIfAbsent(record.className(), key -> new HashSet<>())
                            .add(className);
                }
            });
        } catch (Exception ex) {
            log.warn("Bytecode DAO scanning failed: {}", ex.getMessage());
        }
    }

    private void parseSource(
            Path sourceFile,
            Map<String, DbEntityRecord> entities,
            Map<String, RepositoryDescriptor> repositoryIndex,
            Map<String, Set<String>> usedRepositoryMethods,
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
                if (AnalysisExclusions.isMockClassName(classDeclaration.getNameAsString())) {
                    continue;
                }
                if (classDeclaration.isInterface()) {
                    continue;
                }
                collectRepositoryUsage(classDeclaration, repositoryIndex, usedRepositoryMethods);
                processDaoClass(classDeclaration, packageName, entities, classesByEntity, operationsByClass);
            }
        } catch (IOException | ParseProblemException ex) {
            log.debug("Failed parsing {} for repository metadata: {}", sourceFile, ex.getMessage());
        }
    }

    private void indexRepositoryInterfaces(
            Path sourceRoot,
            Map<String, DbEntityRecord> entities,
            Map<String, RepositoryDescriptor> repositoryIndex) {
        try (Stream<Path> paths = Files.walk(sourceRoot, MAX_WALK_DEPTH, FileVisitOption.FOLLOW_LINKS)) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> !AnalysisExclusions.isExcludedPath(path))
                    .forEach(path -> indexRepositoryInterface(path, entities, repositoryIndex));
        } catch (IOException e) {
            log.warn("Failed traversing {} for repository index: {}", sourceRoot, e.getMessage());
        }
    }

    private void indexRepositoryInterface(
            Path sourceFile,
            Map<String, DbEntityRecord> entities,
            Map<String, RepositoryDescriptor> repositoryIndex) {
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
                if (!(declaration instanceof ClassOrInterfaceDeclaration classDeclaration)
                        || !classDeclaration.isInterface()) {
                    continue;
                }
                Optional<ClassOrInterfaceType> repositoryBase = classDeclaration.getExtendedTypes().stream()
                        .filter(type -> isRepositoryBase(type.getName().getIdentifier()))
                        .findFirst();
                if (repositoryBase.isEmpty()) {
                    continue;
                }
                String repositoryName = classDeclaration.getNameAsString();
                String repositoryFqn = classDeclaration.getFullyQualifiedName()
                        .orElseGet(() -> composeFqn(packageName, repositoryName));

                String entityType = resolveEntityType(repositoryBase.get());
                if (entityType == null) {
                    log.debug("Unable to resolve entity type for repository {}", repositoryFqn);
                    continue;
                }
                String entitySimpleName = simpleName(entityType);
                DbEntityRecord entityRecord = entities.get(entitySimpleName);
                Map<String, String> declaredQueries = new HashMap<>();
                for (MethodDeclaration method : classDeclaration.getMethods()) {
                    extractQuery(method).ifPresent(query -> declaredQueries.put(method.getNameAsString(), query));
                }
                repositoryIndex.putIfAbsent(
                        repositoryName,
                        new RepositoryDescriptor(
                                repositoryFqn,
                                repositoryName,
                                entityRecord,
                                entitySimpleName,
                                declaredQueries));
            }
        } catch (IOException | ParseProblemException ex) {
            log.debug("Failed parsing {} for repository index: {}", sourceFile, ex.getMessage());
        }
    }

    private void appendRepositoryOperations(
            Map<String, RepositoryDescriptor> repositoryIndex,
            Map<String, Set<String>> usedRepositoryMethods,
            Map<String, Set<String>> classesByEntity,
            Map<String, List<DaoOperationRecord>> operationsByClass) {
        if (repositoryIndex.isEmpty()) {
            return;
        }
        for (RepositoryDescriptor descriptor : repositoryIndex.values()) {
            Set<String> used = usedRepositoryMethods.getOrDefault(descriptor.simpleName(), Set.of());
            if (used.isEmpty()) {
                continue;
            }
            DbEntityRecord entityRecord = descriptor.entityRecord();
            String targetDescriptor = buildTargetDescriptor(entityRecord, descriptor.entitySimpleName());
            for (String methodName : used) {
                if (methodName == null || methodName.isBlank()) {
                    continue;
                }
                String querySnippet = descriptor.declaredQueries().get(methodName);
                Optional<String> query = querySnippet == null ? Optional.empty() : Optional.of(querySnippet);
                String operationType = inferOperationType(methodName, query);
                if ("UNKNOWN".equals(operationType)) {
                    continue;
                }
                DaoOperationRecord record = new DaoOperationRecord(
                        descriptor.fullyQualifiedName(),
                        methodName,
                        operationType,
                        targetDescriptor,
                        querySnippet);
                operationsByClass
                        .computeIfAbsent(descriptor.fullyQualifiedName(), key -> new ArrayList<>())
                        .add(record);
            }
            if (entityRecord != null) {
                classesByEntity
                        .computeIfAbsent(entityRecord.className(), key -> new HashSet<>())
                        .add(descriptor.fullyQualifiedName());
            }
        }
    }

    private void collectRepositoryUsage(
            ClassOrInterfaceDeclaration declaration,
            Map<String, RepositoryDescriptor> repositoryIndex,
            Map<String, Set<String>> usedRepositoryMethods) {
        if (repositoryIndex.isEmpty()) {
            return;
        }
        Map<String, String> fieldTypes = new HashMap<>();
        for (FieldDeclaration field : declaration.getFields()) {
            String typeName = simpleName(stripTypeArguments(field.getElementType().asString()));
            if (repositoryIndex.containsKey(typeName)) {
                field.getVariables().forEach(variable -> fieldTypes.put(variable.getNameAsString(), typeName));
            }
        }
        for (MethodDeclaration method : declaration.getMethods()) {
            if (method.getBody().isEmpty()) {
                continue;
            }
            Map<String, String> knownTypes = new HashMap<>(fieldTypes);
            method.getParameters().forEach(param -> {
                String typeName = simpleName(stripTypeArguments(param.getType().asString()));
                if (repositoryIndex.containsKey(typeName)) {
                    knownTypes.put(param.getNameAsString(), typeName);
                }
            });
            method.findAll(VariableDeclarationExpr.class).forEach(local -> {
                String typeName = simpleName(stripTypeArguments(local.getElementType().asString()));
                if (!repositoryIndex.containsKey(typeName)) {
                    return;
                }
                local.getVariables().forEach(variable -> knownTypes.put(variable.getNameAsString(), typeName));
            });
            method.findAll(MethodCallExpr.class).forEach(call -> {
                Optional<Expression> scope = call.getScope();
                if (scope.isEmpty()) {
                    return;
                }
                String repoSimpleName = resolveRepositoryFromScope(scope.get(), knownTypes);
                if (repoSimpleName == null) {
                    return;
                }
                usedRepositoryMethods
                        .computeIfAbsent(repoSimpleName, key -> new HashSet<>())
                        .add(call.getNameAsString());
            });
        }
    }

    private String resolveRepositoryFromScope(Expression scope, Map<String, String> knownTypes) {
        if (scope instanceof NameExpr nameExpr) {
            return knownTypes.get(nameExpr.getNameAsString());
        }
        if (scope instanceof FieldAccessExpr fieldAccess) {
            Expression qualifier = fieldAccess.getScope();
            if (qualifier.isThisExpr() || qualifier.isSuperExpr()) {
                return knownTypes.get(fieldAccess.getNameAsString());
            }
        }
        return null;
    }

    private void processDaoClass(
            ClassOrInterfaceDeclaration declaration,
            String packageName,
            Map<String, DbEntityRecord> entities,
            Map<String, Set<String>> classesByEntity,
            Map<String, List<DaoOperationRecord>> operationsByClass) {
        String className = declaration.getNameAsString();
        String classFqn = declaration.getFullyQualifiedName()
                .orElseGet(() -> composeFqn(packageName, className));

        boolean allowNameFallback = isDaoCandidate(declaration);
        DbEntityRecord entityRecord = resolveEntityForDaoClass(declaration, entities);
        String targetFallback = entityRecord != null ? entityRecord.className() : stripDaoSuffix(className);
        String targetDescriptor = buildTargetDescriptor(entityRecord, targetFallback);

        List<DaoOperationRecord> operations =
                collectDaoOperations(declaration, classFqn, targetDescriptor, allowNameFallback);
        if (operations.isEmpty()) {
            return;
        }

        operationsByClass
                .computeIfAbsent(classFqn, key -> new ArrayList<>())
                .addAll(operations);
        if (entityRecord != null) {
            classesByEntity
                    .computeIfAbsent(entityRecord.className(), key -> new HashSet<>())
                    .add(classFqn);
        }
    }

    private List<DaoOperationRecord> collectDaoOperations(
            ClassOrInterfaceDeclaration declaration,
            String classFqn,
            String targetDescriptor,
            boolean allowNameFallback) {
        List<DaoOperationRecord> operations = new ArrayList<>();
        for (MethodDeclaration method : declaration.getMethods()) {
            if (method.isStatic() || method.getBody().isEmpty()) {
                continue;
            }
            DaoOperationEvidence evidence = inspectDaoMethod(method);
            String operationType = evidence.operationType();
            String querySnippet = evidence.querySnippet();
            if (operationType == null && allowNameFallback) {
                String inferred = inferOperationType(method.getNameAsString(), Optional.empty());
                if (!"UNKNOWN".equals(inferred)) {
                    operationType = inferred;
                }
            }
            if (operationType == null) {
                continue;
            }
            operations.add(new DaoOperationRecord(
                    classFqn,
                    method.getNameAsString(),
                    operationType,
                    targetDescriptor,
                    querySnippet));
        }
        return operations;
    }

    private DaoOperationEvidence inspectDaoMethod(MethodDeclaration method) {
        boolean hasInsertUpdate = false;
        boolean hasUpdate = false;
        boolean hasDelete = false;
        boolean hasSelect = false;
        String querySnippet = null;

        for (MethodCallExpr call : method.findAll(MethodCallExpr.class)) {
            String callName = call.getNameAsString().toLowerCase(Locale.ROOT);
            if (!isLikelyDbScope(call.getScope())) {
                continue;
            }
            if (DAO_DELETE_METHODS.contains(callName)) {
                hasDelete = true;
            }
            if (DAO_UPDATE_METHODS.contains(callName)) {
                hasUpdate = true;
            }
            if (DAO_INSERT_UPDATE_METHODS.contains(callName)) {
                hasInsertUpdate = true;
            }
            if (DAO_SELECT_METHODS.contains(callName)) {
                hasSelect = true;
            }
            if (querySnippet == null && isQueryMethod(callName)) {
                querySnippet = extractQuerySnippet(call);
            }
        }

        String operationType = null;
        if (hasDelete) {
            operationType = "DELETE";
        } else if (hasUpdate) {
            operationType = "UPDATE";
        } else if (hasInsertUpdate) {
            operationType = "INSERT_OR_UPDATE";
        } else if (hasSelect) {
            operationType = "SELECT";
        }
        return new DaoOperationEvidence(operationType, querySnippet);
    }

    private boolean isLikelyDbScope(Optional<Expression> scope) {
        if (scope.isEmpty()) {
            return false;
        }
        String candidate = scope.get().toString().toLowerCase(Locale.ROOT);
        for (String hint : DAO_SCOPE_HINTS) {
            if (candidate.contains(hint)) {
                return true;
            }
        }
        return false;
    }

    private boolean isQueryMethod(String callName) {
        return "createquery".equals(callName)
                || "createnativequery".equals(callName)
                || "createsqlquery".equals(callName);
    }

    private String extractQuerySnippet(MethodCallExpr call) {
        if (call.getArguments().isEmpty()) {
            return null;
        }
        Expression first = call.getArgument(0);
        if (first instanceof StringLiteralExpr literal) {
            return literal.getValue();
        }
        return null;
    }

    private boolean isDaoCandidate(ClassOrInterfaceDeclaration declaration) {
        String name = declaration.getNameAsString().toLowerCase(Locale.ROOT);
        if (name.endsWith("dao") || name.endsWith("repository") || name.endsWith("repo")) {
            return true;
        }
        if (hasAnyAnnotation(declaration, DAO_CLASS_ANNOTATIONS)) {
            return true;
        }
        for (FieldDeclaration field : declaration.getFields()) {
            String fieldType = field.getElementType().asString().toLowerCase(Locale.ROOT);
            for (String daoType : DAO_FIELD_TYPES) {
                if (fieldType.contains(daoType)) {
                    return true;
                }
            }
        }
        return false;
    }

    private DbEntityRecord resolveEntityForDaoClass(
            ClassOrInterfaceDeclaration declaration, Map<String, DbEntityRecord> entities) {
        if (entities == null || entities.isEmpty()) {
            return null;
        }
        for (ClassOrInterfaceType type : declaration.getExtendedTypes()) {
            String entityType = resolveEntityType(type);
            if (entityType != null) {
                DbEntityRecord record = entities.get(simpleName(entityType));
                if (record != null) {
                    return record;
                }
            }
        }
        for (ClassOrInterfaceType type : declaration.getImplementedTypes()) {
            String entityType = resolveEntityType(type);
            if (entityType != null) {
                DbEntityRecord record = entities.get(simpleName(entityType));
                if (record != null) {
                    return record;
                }
            }
        }
        String stripped = stripDaoSuffix(declaration.getNameAsString());
        return entities.get(stripped);
    }

    private DbEntityRecord resolveEntityForDaoClassName(String className, Map<String, DbEntityRecord> entities) {
        if (entities == null || entities.isEmpty() || className == null || className.isBlank()) {
            return null;
        }
        String simple = simpleName(className);
        String stripped = stripDaoSuffix(simple);
        return entities.get(stripped);
    }

    private String stripDaoSuffix(String className) {
        if (className == null || className.isBlank()) {
            return className;
        }
        String stripped = className;
        String[] suffixes = {"Impl", "Repository", "Repo", "DAO", "Dao"};
        boolean updated;
        do {
            updated = false;
            for (String suffix : suffixes) {
                if (stripped.endsWith(suffix) && stripped.length() > suffix.length()) {
                    stripped = stripped.substring(0, stripped.length() - suffix.length());
                    updated = true;
                }
            }
        } while (updated);
        return stripped;
    }

    private boolean hasAnyAnnotation(ClassOrInterfaceDeclaration declaration, Set<String> annotationNames) {
        if (declaration == null || annotationNames == null || annotationNames.isEmpty()) {
            return false;
        }
        Set<String> targets = annotationNames.stream()
                .map(name -> name.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
        return declaration.getAnnotations().stream()
                .map(annotation -> annotation.getName().getIdentifier().toLowerCase(Locale.ROOT))
                .anyMatch(candidate -> targets.contains(candidate)
                        || targets.stream().anyMatch(candidate::endsWith));
    }

    private void mergeOperations(
            Map<String, List<DaoOperationRecord>> target,
            Map<String, List<DaoOperationRecord>> source) {
        source.forEach((className, operations) -> {
            List<DaoOperationRecord> existing =
                    target.computeIfAbsent(className, key -> new ArrayList<>());
            Set<String> existingKeys = existing.stream()
                    .map(this::operationKey)
                    .collect(Collectors.toSet());
            for (DaoOperationRecord operation : operations) {
                String key = operationKey(operation);
                if (existingKeys.add(key)) {
                    existing.add(operation);
                }
            }
        });
    }

    private String operationKey(DaoOperationRecord record) {
        return record.methodName() + "|" + record.operationType() + "|" + record.target();
    }

    private record DaoOperationEvidence(String operationType, String querySnippet) {}

    private record RepositoryDescriptor(
            String fullyQualifiedName,
            String simpleName,
            DbEntityRecord entityRecord,
            String entitySimpleName,
            Map<String, String> declaredQueries) {}

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

    private String stripTypeArguments(String typeName) {
        if (typeName == null || typeName.isBlank()) {
            return typeName;
        }
        int idx = typeName.indexOf('<');
        return idx > 0 ? typeName.substring(0, idx) : typeName;
    }

    private String composeFqn(String packageName, String simpleName) {
        if (packageName == null || packageName.isBlank()) {
            return simpleName;
        }
        return packageName + "." + simpleName;
    }
}
