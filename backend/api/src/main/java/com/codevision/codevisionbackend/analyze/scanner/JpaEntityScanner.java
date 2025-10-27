package com.codevision.codevisionbackend.analyze.scanner;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseProblemException;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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
public class JpaEntityScanner {

    private static final Logger log = LoggerFactory.getLogger(JpaEntityScanner.class);
    private static final Set<String> RELATIONSHIP_ANNOTATIONS = Set.of(
            "onetomany", "manytoone", "manytomany", "onetoone");
    private static final Map<String, String> RELATIONSHIP_LABELS = Map.of(
            "onetomany", "ONE_TO_MANY",
            "manytoone", "MANY_TO_ONE",
            "manytomany", "MANY_TO_MANY",
            "onetoone", "ONE_TO_ONE");
    private static final String ENTITY_ANNOTATION = "entity";
    private static final String TABLE_ANNOTATION = "table";
    private static final String COLUMN_ANNOTATION = "column";
    private static final Set<String> PRIMARY_KEY_ANNOTATIONS = Set.of("id", "embeddedid");
    private static final int MAX_WALK_DEPTH = Integer.MAX_VALUE;

    private final JavaParser javaParser;

    public JpaEntityScanner() {
        ParserConfiguration configuration = new ParserConfiguration()
                .setLanguageLevel(ParserConfiguration.LanguageLevel.BLEEDING_EDGE);
        this.javaParser = new JavaParser(configuration);
    }

    public List<DbEntityRecord> scan(Path repoRoot, List<Path> moduleRoots) {
        if (repoRoot == null || moduleRoots == null || moduleRoots.isEmpty()) {
            return List.of();
        }

        Set<Path> visitedSourceRoots = new HashSet<>();
        List<DbEntityRecord> entities = new ArrayList<>();

        for (Path moduleRoot : moduleRoots) {
            Path sourceRoot = moduleRoot.resolve("src/main/java").toAbsolutePath().normalize();
            if (!Files.isDirectory(sourceRoot) || !visitedSourceRoots.add(sourceRoot)) {
                continue;
            }
            traverseSourceRoot(sourceRoot, entities);
        }

        return entities;
    }

    private void traverseSourceRoot(Path sourceRoot, List<DbEntityRecord> collector) {
        try (Stream<Path> paths = Files.walk(sourceRoot, MAX_WALK_DEPTH, FileVisitOption.FOLLOW_LINKS)) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .forEach(path -> parseSource(path, collector));
        } catch (IOException e) {
            log.warn("Failed traversing {} for entity scanning: {}", sourceRoot, e.getMessage());
        }
    }

    private void parseSource(Path sourceFile, List<DbEntityRecord> collector) {
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
                        || classDeclaration.isInterface()) {
                    continue;
                }
                if (!hasAnnotation(classDeclaration, ENTITY_ANNOTATION)) {
                    continue;
                }
                collector.add(toRecord(packageName, classDeclaration));
            }
        } catch (IOException | ParseProblemException ex) {
            log.debug("Failed parsing {} for entity metadata: {}", sourceFile, ex.getMessage());
        }
    }

    private DbEntityRecord toRecord(String packageName, ClassOrInterfaceDeclaration declaration) {
        String className = declaration.getNameAsString();
        String fqName = declaration.getFullyQualifiedName()
                .orElseGet(() -> composeFqn(packageName, className));
        String tableName = resolveTableName(declaration, className);

        List<String> primaryKeys = new ArrayList<>();
        List<DbEntityRecord.EntityField> fields = new ArrayList<>();
        List<DbEntityRecord.EntityRelationship> relationships = new ArrayList<>();

        for (FieldDeclaration field : declaration.getFields()) {
            String columnName = resolveColumnName(field);
            boolean primaryKey = hasAnyAnnotation(field, PRIMARY_KEY_ANNOTATIONS);
            Optional<String> relationshipType = resolveRelationship(field);
            String targetType = null;

            for (var variable : field.getVariables()) {
                String fieldName = variable.getNameAsString();
                String typeName = variable.getType().asString();
                fields.add(new DbEntityRecord.EntityField(fieldName, typeName, columnName));
                if (primaryKey) {
                    primaryKeys.add(fieldName);
                }
                if (relationshipType.isPresent()) {
                    targetType = resolveTargetType(variable.getType());
                    relationships.add(new DbEntityRecord.EntityRelationship(
                            fieldName, targetType, relationshipType.get()));
                }
            }
        }

        return new DbEntityRecord(className, fqName, tableName, primaryKeys, fields, relationships);
    }

    private boolean hasAnnotation(NodeWithAnnotations<?> node, String simpleName) {
        String normalized = simpleName.toLowerCase(Locale.ROOT);
        for (AnnotationExpr annotation : node.getAnnotations()) {
            String candidate = annotation.getName().getIdentifier().toLowerCase(Locale.ROOT);
            if (candidate.equals(normalized) || candidate.endsWith("." + normalized)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasAnyAnnotation(NodeWithAnnotations<?> node, Set<String> annotationNames) {
        Set<String> targets = annotationNames.stream()
                .map(name -> name.toLowerCase(Locale.ROOT))
                .collect(java.util.stream.Collectors.toSet());
        return node.getAnnotations().stream()
                .map(annotation -> annotation.getName().getIdentifier().toLowerCase(Locale.ROOT))
                .anyMatch(candidate -> targets.contains(candidate)
                        || targets.stream().anyMatch(candidate::endsWith));
    }

    private String resolveTargetType(Type type) {
        if (type instanceof ClassOrInterfaceType interfaceType) {
            if (interfaceType.getTypeArguments().isPresent() && !interfaceType.getTypeArguments().get().isEmpty()) {
                Type first = interfaceType.getTypeArguments().get().get(0);
                return first.asString();
            }
            return interfaceType.getName().getIdentifier();
        }
        return type.asString();
    }

    private Optional<String> resolveRelationship(FieldDeclaration field) {
        return field.getAnnotations().stream()
                .map(annotation -> annotation.getName().getIdentifier().toLowerCase(Locale.ROOT))
                .filter(RELATIONSHIP_ANNOTATIONS::contains)
                .map(name -> RELATIONSHIP_LABELS.getOrDefault(name, name.toUpperCase(Locale.ROOT)))
                .findFirst();
    }

    private String resolveTableName(NodeWithAnnotations<?> declaration, String defaultName) {
        return findAnnotationValue(declaration, TABLE_ANNOTATION, "name").orElse(defaultName);
    }

    private String resolveColumnName(FieldDeclaration field) {
        return findAnnotationValue(field, COLUMN_ANNOTATION, "name").orElse(null);
    }

    private Optional<String> findAnnotationValue(NodeWithAnnotations<?> node, String annotationName, String key) {
        String normalized = annotationName.toLowerCase(Locale.ROOT);
        for (AnnotationExpr annotation : node.getAnnotations()) {
            String candidate = annotation.getName().getIdentifier().toLowerCase(Locale.ROOT);
            if (!candidate.equals(normalized) && !candidate.endsWith("." + normalized)) {
                continue;
            }
            if (annotation instanceof SingleMemberAnnotationExpr singleMember) {
                return Optional.of(singleMember.getMemberValue().toString().replace("\"", ""));
            }
            if (annotation instanceof NormalAnnotationExpr normalAnnotation) {
                for (MemberValuePair pair : normalAnnotation.getPairs()) {
                    if (pair.getName().asString().equalsIgnoreCase(key)) {
                        return Optional.of(pair.getValue().toString().replace("\"", ""));
                    }
                }
            }
        }
        return Optional.empty();
    }

    private String composeFqn(String packageName, String simpleName) {
        if (packageName == null || packageName.isBlank()) {
            return simpleName;
        }
        return packageName + "." + simpleName;
    }
}
