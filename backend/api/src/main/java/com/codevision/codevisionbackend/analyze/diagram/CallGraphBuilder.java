package com.codevision.codevisionbackend.analyze.diagram;

import com.codevision.codevisionbackend.analyze.diagram.CallGraph.GraphNode;
import com.codevision.codevisionbackend.analyze.scanner.ClassMetadataRecord;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseProblemException;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.type.ArrayType;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.IntersectionType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.UnionType;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CallGraphBuilder {

    private static final Logger log = LoggerFactory.getLogger(CallGraphBuilder.class);
    private final JavaParser javaParser;

    public CallGraphBuilder() {
        ParserConfiguration configuration = new ParserConfiguration()
                .setLanguageLevel(ParserConfiguration.LanguageLevel.BLEEDING_EDGE);
        this.javaParser = new JavaParser(configuration);
    }

    public CallGraph build(Path repoRoot, List<ClassMetadataRecord> classRecords) {
        if (repoRoot == null || classRecords == null || classRecords.isEmpty()) {
            return new CallGraph.Builder().build();
        }

        Map<String, ClassMetadataRecord> byFqn = classRecords.stream()
                .filter(record -> record.fullyQualifiedName() != null)
                .collect(Collectors.toMap(
                        ClassMetadataRecord::fullyQualifiedName,
                        record -> record,
                        (left, right) -> left,
                        LinkedHashMap::new));
        Map<String, List<ClassMetadataRecord>> bySimpleName = classRecords.stream()
                .filter(record -> record.className() != null)
                .collect(Collectors.groupingBy(
                        ClassMetadataRecord::className, LinkedHashMap::new, Collectors.toList()));
        Map<String, List<ClassMetadataRecord>> byPath = classRecords.stream()
                .filter(record -> record.relativePath() != null && !record.relativePath().isBlank())
                .collect(Collectors.groupingBy(
                        ClassMetadataRecord::relativePath, LinkedHashMap::new, Collectors.toList()));

        CallGraph.Builder builder = new CallGraph.Builder();
        classRecords.forEach(record -> builder.addNode(new GraphNode(
                record.fullyQualifiedName(),
                record.className(),
                record.stereotype(),
                record.userCode(),
                false)));

        byPath.forEach((relativePath, recordsInFile) -> {
            Path sourceFile = repoRoot.resolve(relativePath).normalize();
            Optional<CompilationUnit> unit = parse(sourceFile);
            if (unit.isEmpty()) {
                return;
            }
            Map<String, TypeDeclaration<?>> typesByName = unit.get().getTypes().stream()
                    .collect(Collectors.toMap(
                            TypeDeclaration::getNameAsString,
                            type -> type,
                            (left, right) -> left,
                            LinkedHashMap::new));
            Map<String, String> explicitImports = buildExplicitImportMap(unit.get());
            Set<String> wildcardImports = buildWildcardImports(unit.get());
            String packageName = unit.get().getPackageDeclaration()
                    .map(pkg -> pkg.getName().asString())
                    .orElse("");

            for (ClassMetadataRecord record : recordsInFile) {
                TypeDeclaration<?> declaration = typesByName.get(record.className());
                if (declaration == null) {
                    continue;
                }
                Set<String> referencedTypes = collectReferencedTypes(declaration);
                for (String token : referencedTypes) {
                    String resolved = resolveTypeName(
                            token, packageName, explicitImports, wildcardImports, bySimpleName);
                    if (resolved == null || resolved.equals(record.fullyQualifiedName())) {
                        continue;
                    }
                    boolean known = byFqn.containsKey(resolved);
                    if (!known) {
                        if (!resolved.contains("codeviz2")) {
                            continue;
                        }
                        builder.addNode(new GraphNode(
                                resolved, simpleName(resolved), "EXTERNAL", false, true));
                    }
                    builder.addEdge(record.fullyQualifiedName(), resolved);
                }
            }
        });

        return builder.build();
    }

    private Optional<CompilationUnit> parse(Path sourceFile) {
        if (sourceFile == null || !Files.exists(sourceFile)) {
            return Optional.empty();
        }
        try {
            return javaParser.parse(sourceFile).getResult();
        } catch (IOException e) {
            log.debug("Failed to read {}: {}", sourceFile, e.getMessage());
            return Optional.empty();
        } catch (ParseProblemException e) {
            log.debug("Failed to parse {}: {}", sourceFile, e.getMessage());
            return Optional.empty();
        }
    }

    private Map<String, String> buildExplicitImportMap(CompilationUnit unit) {
        return unit.getImports().stream()
                .filter(importDecl -> !importDecl.isAsterisk() && !importDecl.isStatic())
                .collect(Collectors.toMap(
                        importDecl -> importDecl.getName().getIdentifier(),
                        importDecl -> importDecl.getName().asString(),
                        (left, right) -> left,
                        LinkedHashMap::new));
    }

    private Set<String> buildWildcardImports(CompilationUnit unit) {
        return unit.getImports().stream()
                .filter(importDecl -> importDecl.isAsterisk() && !importDecl.isStatic())
                .map(ImportDeclaration::getName)
                .map(name -> name.asString())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<String> collectReferencedTypes(TypeDeclaration<?> declaration) {
        Set<String> identifiers = new LinkedHashSet<>();
        if (declaration instanceof ClassOrInterfaceDeclaration clazz) {
            clazz.getExtendedTypes().forEach(type -> identifiers.add(type.getNameWithScope()));
            clazz.getImplementedTypes().forEach(type -> identifiers.add(type.getNameWithScope()));
            clazz.getFields().forEach(field -> collectTypeIdentifiers(field.getElementType(), identifiers));
            clazz.getConstructors().forEach(constructor -> collectParameterTypes(constructor, identifiers));
            clazz.getMethods().forEach(method -> {
                collectTypeIdentifiers(method.getType(), identifiers);
                collectParameterTypes(method, identifiers);
            });
        } else if (declaration instanceof RecordDeclaration recordDeclaration) {
            recordDeclaration.getParameters().forEach(param -> collectTypeIdentifiers(param.getType(), identifiers));
        }
        return identifiers;
    }

    private void collectParameterTypes(ConstructorDeclaration declaration, Set<String> collector) {
        declaration.getParameters().forEach(param -> collectTypeIdentifiers(param.getType(), collector));
    }

    private void collectParameterTypes(MethodDeclaration declaration, Set<String> collector) {
        declaration.getParameters().forEach(param -> collectTypeIdentifiers(param.getType(), collector));
    }

    private void collectTypeIdentifiers(Type type, Set<String> collector) {
        if (type == null) {
            return;
        }
        if (type.isPrimitiveType() || type.isVoidType()) {
            return;
        }
        if (type.isArrayType()) {
            collectTypeIdentifiers(type.asArrayType().getComponentType(), collector);
            return;
        }
        if (type.isUnionType()) {
            for (Type element : type.asUnionType().getElements()) {
                collectTypeIdentifiers(element, collector);
            }
            return;
        }
        if (type.isIntersectionType()) {
            for (Type element : type.asIntersectionType().getElements()) {
                collectTypeIdentifiers(element, collector);
            }
            return;
        }
        if (type.isClassOrInterfaceType()) {
            ClassOrInterfaceType ci = type.asClassOrInterfaceType();
            collector.add(ci.getNameWithScope());
            ci.getTypeArguments().ifPresent(args -> args.forEach(arg -> collectTypeIdentifiers(arg, collector)));
            return;
        }
        if (type.isVarType()) {
            return;
        }
        if (type instanceof ArrayType arrayType) {
            collectTypeIdentifiers(arrayType.getComponentType(), collector);
            return;
        }
        collector.add(type.asString());
    }

    private String resolveTypeName(
            String token,
            String currentPackage,
            Map<String, String> explicitImports,
            Set<String> wildcardImports,
            Map<String, List<ClassMetadataRecord>> simpleIndex) {
        if (token == null || token.isBlank()) {
            return null;
        }
        String cleaned = cleanToken(token);
        if (cleaned.contains(".")) {
            return cleaned;
        }
        String imported = explicitImports.get(cleaned);
        if (imported != null) {
            return imported;
        }
        if (currentPackage != null && !currentPackage.isBlank()) {
            String candidate = currentPackage + "." + cleaned;
            if (matchesSimpleIndex(cleaned, candidate, simpleIndex)) {
                return candidate;
            }
        }
        for (String wildcard : wildcardImports) {
            String candidate = wildcard + "." + cleaned;
            if (matchesSimpleIndex(cleaned, candidate, simpleIndex)) {
                return candidate;
            }
        }
        List<ClassMetadataRecord> candidates = simpleIndex.getOrDefault(cleaned, List.of());
        if (candidates.size() == 1) {
            return candidates.getFirst().fullyQualifiedName();
        }
        return cleaned;
    }

    private boolean matchesSimpleIndex(
            String simpleName,
            String candidateFqn,
            Map<String, List<ClassMetadataRecord>> simpleIndex) {
        List<ClassMetadataRecord> candidates = simpleIndex.get(simpleName);
        if (candidates == null) {
            return false;
        }
        return candidates.stream().anyMatch(record -> candidateFqn.equals(record.fullyQualifiedName()));
    }

    private String simpleName(String fullyQualifiedName) {
        if (fullyQualifiedName == null) {
            return null;
        }
        int lastDot = fullyQualifiedName.lastIndexOf('.');
        if (lastDot >= 0 && lastDot < fullyQualifiedName.length() - 1) {
            return fullyQualifiedName.substring(lastDot + 1);
        }
        return fullyQualifiedName;
    }

    private String cleanToken(String token) {
        String normalized = token.trim();
        int genericStart = normalized.indexOf('<');
        if (genericStart > 0) {
            normalized = normalized.substring(0, genericStart);
        }
        normalized = normalized.replace("[]", "");
        return normalized;
    }
}
