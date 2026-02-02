package com.codevision.codevisionbackend.analyze.diagram;

import com.codevision.codevisionbackend.analyze.scanner.ApiEndpointRecord;
import com.codevision.codevisionbackend.analyze.scanner.ClassMetadataRecord;
import com.codevision.codevisionbackend.analyze.scanner.DbAnalysisResult;
import com.codevision.codevisionbackend.analyze.scanner.DbEntityRecord;
import com.codevision.codevisionbackend.project.diagram.DiagramType;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class DiagramBuilderService {

    private static final int MAX_CLASS_NODES = 30;
    private static final int MAX_COMPONENT_EDGES = 40;
    private static final int MAX_ENDPOINTS = 6;
    private static final int MAX_SEQUENCE_DEPTH = 8;
    private static final String ROOT_MODULE_KEY = "/";

    private final CallGraphBuilder callGraphBuilder;

    public DiagramBuilderService(CallGraphBuilder callGraphBuilder) {
        this.callGraphBuilder = callGraphBuilder;
    }

    public DiagramGenerationResult generate(
            Path repoRoot,
            List<ClassMetadataRecord> classRecords,
            List<ApiEndpointRecord> apiEndpoints,
            DbAnalysisResult dbAnalysisResult,
            List<Path> moduleRoots) {
        CallGraph graph = callGraphBuilder.build(repoRoot, classRecords);
        ControlFlowSequenceBuilder sequenceBuilder =
                new ControlFlowSequenceBuilder(repoRoot, moduleRoots, classRecords, MAX_SEQUENCE_DEPTH);
        List<ApiEndpointRecord> limitedEndpoints = selectEndpoints(apiEndpoints, MAX_ENDPOINTS);
        List<DiagramDefinition> diagrams = new ArrayList<>();
        Map<String, List<String>> callFlows = buildCallFlows(apiEndpoints, sequenceBuilder, MAX_ENDPOINTS * 2);
        ModuleMapping moduleMapping = buildModuleMapping(repoRoot, moduleRoots, classRecords);

        Optional.ofNullable(buildClassDiagram("Core Class Relationships", classRecords, graph, MAX_CLASS_NODES))
                .ifPresent(diagrams::add);
        if (classRecords != null && classRecords.size() > MAX_CLASS_NODES) {
            Optional.ofNullable(buildClassDiagram(
                    "Full Class Relationships", classRecords, graph, 0))
                    .ifPresent(diagrams::add);
        }
        Optional.ofNullable(buildComponentDiagram(classRecords, graph)).ifPresent(diagrams::add);
        Optional.ofNullable(buildModuleComponentDiagram(moduleMapping, graph)).ifPresent(diagrams::add);
        diagrams.addAll(buildModuleClassDiagrams(moduleMapping, graph));
        Optional.ofNullable(buildUseCaseDiagram(apiEndpoints)).ifPresent(diagrams::add);
        Optional.ofNullable(buildErdDiagram(dbAnalysisResult)).ifPresent(diagrams::add);
        Optional.ofNullable(buildDbSchemaDiagram(dbAnalysisResult)).ifPresent(diagrams::add);
        diagrams.addAll(buildSequenceDiagrams(limitedEndpoints, sequenceBuilder, dbAnalysisResult, false));
        diagrams.addAll(buildSequenceDiagrams(limitedEndpoints, sequenceBuilder, dbAnalysisResult, true));

        return new DiagramGenerationResult(diagrams, callFlows);
    }

    private DiagramDefinition buildClassDiagram(
            String title,
            List<ClassMetadataRecord> classes,
            CallGraph graph,
            int limit) {
        if (classes == null || classes.isEmpty()) {
            return null;
        }
        List<ClassMetadataRecord> selected = selectClassesForDiagram(classes, graph, limit);
        if (selected.isEmpty()) {
            return null;
        }
        Map<String, String> aliases = buildAliases(selected.stream()
                .map(ClassMetadataRecord::fullyQualifiedName)
                .collect(Collectors.toList()));

        StringBuilder plantuml = new StringBuilder("@startuml\n");
        plantuml.append("skinparam shadowing false\n");
        plantuml.append("skinparam monochrome true\n");
        plantuml.append("hide empty members\n");
        plantuml.append("set namespaceSeparator none\n");

        selected.forEach(record -> {
            String alias = aliases.get(record.fullyQualifiedName());
            plantuml.append("class \"")
                    .append(record.className())
                    .append("\" as ")
                    .append(alias);
            if (record.stereotype() != null && !record.stereotype().isBlank()) {
                plantuml.append(" <<")
                        .append(record.stereotype())
                        .append(">>");
            }
            plantuml.append("\n");
        });

        Set<String> edges = new LinkedHashSet<>();
        for (ClassMetadataRecord record : selected) {
            Set<String> targets = graph.targets(record.fullyQualifiedName());
            targets.stream()
                    .filter(target -> aliases.containsKey(target))
                    .forEach(target -> edges.add(record.fullyQualifiedName() + "->" + target));
        }
        edges.forEach(edge -> {
            String[] parts = edge.split("->");
            plantuml.append(aliases.get(parts[0]))
                    .append(" --> ")
                    .append(aliases.get(parts[1]))
                    .append(" : uses\n");
        });
        plantuml.append("@enduml");

        StringBuilder mermaid = new StringBuilder("classDiagram\n");
        selected.forEach(record -> {
            mermaid.append("    class ")
                    .append(aliases.get(record.fullyQualifiedName()))
                    .append(" {\n")
                    .append("        <<")
                    .append(record.stereotype() == null ? "CLASS" : record.stereotype())
                    .append(">>\n")
                    .append("    }\n");
        });
        edges.forEach(edge -> {
            String[] parts = edge.split("->");
            mermaid.append("    ")
                    .append(aliases.get(parts[0]))
                    .append(" --> ")
                    .append(aliases.get(parts[1]))
                    .append(" : uses\n");
        });

        if (edges.isEmpty()) {
            List<HeuristicEdge> heuristicEdges = buildHeuristicEdges(selected, aliases);
            heuristicEdges.forEach(edge -> {
                plantuml.append(edge.fromAlias())
                        .append(" --> ")
                        .append(edge.toAlias());
                if (edge.label() != null && !edge.label().isBlank()) {
                    plantuml.append(" : ").append(edge.label());
                }
                plantuml.append("\n");
                mermaid.append("    ")
                        .append(edge.fromAlias())
                        .append(" --> ")
                        .append(edge.toAlias());
                if (edge.label() != null && !edge.label().isBlank()) {
                    mermaid.append(" : ").append(edge.label());
                }
                mermaid.append("\n");
            });
        }

        return new DiagramDefinition(
                DiagramType.CLASS, title, plantuml.toString(), mermaid.toString(), Map.of());
    }

    private DiagramDefinition buildComponentDiagram(List<ClassMetadataRecord> classes, CallGraph graph) {
        if (classes == null || classes.isEmpty()) {
            return null;
        }
        Map<String, List<ClassMetadataRecord>> recordsByStereo = groupClassesByStereotype(classes);
        if (recordsByStereo.isEmpty()) {
            return null;
        }
        Map<String, Set<String>> stereotypeToClasses = recordsByStereo.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().stream()
                                .map(ClassMetadataRecord::fullyQualifiedName)
                                .collect(Collectors.toCollection(LinkedHashSet::new)),
                        (left, right) -> left,
                        LinkedHashMap::new));

        List<String> stereotypeOrder = List.of("CONTROLLER", "SERVICE", "REPOSITORY", "ENTITY", "CONFIG", "UTILITY", "TEST", "OTHER");
        Map<String, Set<String>> orderedClasses = stereotypeToClasses.entrySet().stream()
                .sorted(Comparator.comparing(entry -> {
                    int idx = stereotypeOrder.indexOf(entry.getKey());
                    return idx < 0 ? Integer.MAX_VALUE : idx;
                }))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (left, right) -> left,
                        LinkedHashMap::new));

        Set<Edge> componentEdges = new LinkedHashSet<>();
        orderedClasses.forEach((stereotype, values) -> values.forEach(cls -> {
            Set<String> targets = graph.targets(cls);
            Set<String> normalizedTargets = targets.stream()
                    .map(target -> findStereotype(target, classes))
                    .filter(Objects::nonNull)
                    .filter(stereo -> !stereo.equals(stereotype))
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            normalizedTargets.stream()
                    .limit(MAX_COMPONENT_EDGES)
                    .map(targetStereo -> new Edge(stereotype, targetStereo))
                    .forEach(componentEdges::add);
        }));

        StringBuilder plantuml = new StringBuilder("@startuml\n");
        plantuml.append("skinparam rectangleStyle rounded\n");
        plantuml.append("skinparam shadowing false\n");
        orderedClasses.keySet().forEach(stereotype -> plantuml
                .append("rectangle \"")
                .append(componentNodeLabel(stereotype, recordsByStereo.get(stereotype)))
                .append("\" as ")
                .append(stereotype)
                .append("\n"));
        componentEdges.forEach(edge -> plantuml
                .append(edge.from())
                .append(" --> ")
                .append(edge.to())
                .append(" : depends\n"));
        plantuml.append("@enduml");

        StringBuilder mermaid = new StringBuilder("flowchart LR\n");
        orderedClasses.keySet().forEach(stereotype -> mermaid
                .append("    ")
                .append(stereotype)
                .append("[[")
                .append(componentNodeLabel(stereotype, recordsByStereo.get(stereotype)).replace("\\n", "<br/>"))
                .append("]]\n"));
        componentEdges.forEach(edge -> mermaid
                .append("    ")
                .append(edge.from())
                .append(" --> ")
                .append(edge.to())
                .append("\n"));

        return new DiagramDefinition(
                DiagramType.COMPONENT,
                "Component Interactions",
                plantuml.toString(),
                mermaid.toString(),
                Map.of("componentCount", orderedClasses.size()));
    }

    private DiagramDefinition buildModuleComponentDiagram(ModuleMapping mapping, CallGraph graph) {
        if (mapping == null || mapping.moduleKeys().size() <= 1) {
            return null;
        }
        List<String> moduleKeys = mapping.moduleKeys();
        Map<String, String> aliases = buildAliases(moduleKeys);
        Set<Edge> moduleEdges = new LinkedHashSet<>();

        mapping.classToModule().forEach((sourceClass, sourceModule) -> {
            Set<String> targets = graph.targets(sourceClass);
            targets.forEach(target -> {
                String targetModule = mapping.classToModule().get(target);
                if (targetModule == null || targetModule.equals(sourceModule)) {
                    return;
                }
                moduleEdges.add(new Edge(sourceModule, targetModule));
            });
        });

        StringBuilder plantuml = new StringBuilder("@startuml\n");
        plantuml.append("skinparam rectangleStyle rounded\n");
        plantuml.append("skinparam shadowing false\n");
        moduleKeys.forEach(key -> plantuml
                .append("rectangle \"")
                .append(moduleLabel(key, mapping.classesByModule().get(key)))
                .append("\" as ")
                .append(aliases.get(key))
                .append("\n"));
        moduleEdges.forEach(edge -> plantuml
                .append(aliases.get(edge.from()))
                .append(" --> ")
                .append(aliases.get(edge.to()))
                .append(" : depends\n"));
        plantuml.append("@enduml");

        StringBuilder mermaid = new StringBuilder("flowchart LR\n");
        moduleKeys.forEach(key -> mermaid
                .append("    ")
                .append(aliases.get(key))
                .append("[[")
                .append(moduleLabel(key, mapping.classesByModule().get(key)).replace("\\n", "<br/>"))
                .append("]]\n"));
        moduleEdges.forEach(edge -> mermaid
                .append("    ")
                .append(aliases.get(edge.from()))
                .append(" --> ")
                .append(aliases.get(edge.to()))
                .append("\n"));

        return new DiagramDefinition(
                DiagramType.COMPONENT,
                "Module Dependencies",
                plantuml.toString(),
                mermaid.toString(),
                Map.of("moduleCount", moduleKeys.size()));
    }

    private List<DiagramDefinition> buildModuleClassDiagrams(ModuleMapping mapping, CallGraph graph) {
        if (mapping == null || mapping.moduleKeys().size() <= 1) {
            return List.of();
        }
        List<DiagramDefinition> diagrams = new ArrayList<>();
        for (String moduleKey : mapping.moduleKeys()) {
            List<ClassMetadataRecord> moduleClasses = mapping.classesByModule().get(moduleKey);
            if (moduleClasses == null || moduleClasses.isEmpty()) {
                continue;
            }
            String title = "Module Class Relationships: " + moduleLabelPlain(moduleKey);
            DiagramDefinition diagram = buildClassDiagram(title, moduleClasses, graph, 0);
            if (diagram != null) {
                diagrams.add(diagram);
            }
        }
        return diagrams;
    }

    private List<ApiEndpointRecord> selectEndpoints(List<ApiEndpointRecord> endpoints, int limit) {
        if (endpoints == null || endpoints.isEmpty()) {
            return List.of();
        }
        return endpoints.stream().limit(Math.max(limit, 0)).toList();
    }

    private List<HeuristicEdge> buildHeuristicEdges(List<ClassMetadataRecord> classes, Map<String, String> aliases) {
        Map<String, List<ClassMetadataRecord>> byStereotype = classes.stream()
                .collect(Collectors.groupingBy(
                        record -> normalizeStereotype(record.stereotype()), LinkedHashMap::new, Collectors.toList()));
        List<HeuristicEdge> edges = new ArrayList<>();
        addHeuristicEdge(byStereotype, aliases, edges, "CONTROLLER", "SERVICE", "calls");
        addHeuristicEdge(byStereotype, aliases, edges, "SERVICE", "REPOSITORY", "persists");
        addHeuristicEdge(byStereotype, aliases, edges, "REPOSITORY", "ENTITY", "maps");
        addHeuristicEdge(byStereotype, aliases, edges, "SERVICE", "CONFIG", "uses");
        addHeuristicEdge(byStereotype, aliases, edges, "CONTROLLER", "REPOSITORY", "accesses");
        if (edges.isEmpty() && classes.size() > 1) {
            String fromAlias = aliases.get(classes.get(0).fullyQualifiedName());
            String toAlias = aliases.get(classes.get(1).fullyQualifiedName());
            if (fromAlias != null && toAlias != null) {
                edges.add(new HeuristicEdge(fromAlias, toAlias, "related"));
            }
        }
        return edges;
    }

    private void addHeuristicEdge(
            Map<String, List<ClassMetadataRecord>> byStereo,
            Map<String, String> aliases,
            List<HeuristicEdge> edges,
            String fromStereo,
            String toStereo,
            String label) {
        List<ClassMetadataRecord> from = byStereo.get(fromStereo);
        List<ClassMetadataRecord> to = byStereo.get(toStereo);
        if (from == null || from.isEmpty() || to == null || to.isEmpty()) {
            return;
        }
        String fromAlias = aliases.get(from.get(0).fullyQualifiedName());
        String toAlias = aliases.get(to.get(0).fullyQualifiedName());
        if (fromAlias != null && toAlias != null) {
            edges.add(new HeuristicEdge(fromAlias, toAlias, label));
        }
    }

    private record HeuristicEdge(String fromAlias, String toAlias, String label) {}

    private ModuleMapping buildModuleMapping(
            Path repoRoot,
            List<Path> moduleRoots,
            List<ClassMetadataRecord> classes) {
        if (classes == null || classes.isEmpty()) {
            return ModuleMapping.empty();
        }
        Path normalizedRoot = repoRoot == null ? null : repoRoot.toAbsolutePath().normalize();
        List<ModuleKey> moduleKeys = new ArrayList<>();
        if (moduleRoots != null) {
            for (Path moduleRoot : moduleRoots) {
                if (moduleRoot == null || normalizedRoot == null) {
                    continue;
                }
                Path normalizedModule = moduleRoot.toAbsolutePath().normalize();
                if (!normalizedModule.startsWith(normalizedRoot)) {
                    continue;
                }
                Path relative = normalizedRoot.relativize(normalizedModule);
                String key = relative.getNameCount() == 0
                        ? ROOT_MODULE_KEY
                        : relative.toString().replace('\\', '/');
                moduleKeys.add(new ModuleKey(key, relative, relative.getNameCount()));
            }
        }
        if (moduleKeys.stream().noneMatch(key -> ROOT_MODULE_KEY.equals(key.key()))) {
            moduleKeys.add(new ModuleKey(ROOT_MODULE_KEY, Path.of(""), 0));
        }
        moduleKeys = moduleKeys.stream()
                .distinct()
                .sorted(Comparator.comparingInt(ModuleKey::depth).reversed())
                .toList();

        Map<String, List<ClassMetadataRecord>> classesByModule = new LinkedHashMap<>();
        Map<String, String> classToModule = new LinkedHashMap<>();
        for (ClassMetadataRecord record : classes) {
            String moduleKey = detectModuleKey(record.relativePath(), moduleKeys);
            classesByModule
                    .computeIfAbsent(moduleKey, key -> new ArrayList<>())
                    .add(record);
            if (record.fullyQualifiedName() != null) {
                classToModule.put(record.fullyQualifiedName(), moduleKey);
            }
        }
        List<String> orderedModules = classesByModule.keySet().stream()
                .sorted()
                .toList();
        return new ModuleMapping(classesByModule, classToModule, orderedModules);
    }

    private String detectModuleKey(String relativePath, List<ModuleKey> moduleKeys) {
        Path candidate = normalizePath(relativePath);
        for (ModuleKey moduleKey : moduleKeys) {
            if (moduleKey.depth() == 0) {
                continue;
            }
            if (candidate.startsWith(moduleKey.relativePath())) {
                return moduleKey.key();
            }
        }
        return ROOT_MODULE_KEY;
    }

    private Path normalizePath(String value) {
        if (value == null || value.isBlank()) {
            return Path.of("");
        }
        return Path.of(value.replace('\\', '/')).normalize();
    }

    private String moduleLabel(String moduleKey, List<ClassMetadataRecord> classes) {
        int count = classes == null ? 0 : classes.size();
        return moduleLabelPlain(moduleKey) + "\\n" + count + " classes";
    }

    private String moduleLabelPlain(String moduleKey) {
        if (moduleKey == null || moduleKey.isBlank() || ROOT_MODULE_KEY.equals(moduleKey)) {
            return "root";
        }
        return moduleKey;
    }

    private List<ClassMetadataRecord> selectClassesForDiagram(
            List<ClassMetadataRecord> classes,
            CallGraph graph,
            int limit) {
        if (classes == null || classes.isEmpty()) {
            return List.of();
        }
        Map<String, Integer> degree = new HashMap<>();
        graph.edges().forEach((source, targets) -> {
            degree.merge(source, targets.size(), Integer::sum);
            targets.forEach(target -> degree.merge(target, 1, Integer::sum));
        });
        Comparator<ClassMetadataRecord> comparator = Comparator
                .comparingInt((ClassMetadataRecord record) -> degree.getOrDefault(record.fullyQualifiedName(), 0))
                .reversed()
                .thenComparing(ClassMetadataRecord::stereotype, Comparator.nullsLast(String::compareToIgnoreCase))
                .thenComparing(ClassMetadataRecord::packageName, Comparator.nullsLast(String::compareToIgnoreCase))
                .thenComparing(ClassMetadataRecord::className, Comparator.nullsLast(String::compareToIgnoreCase));
        List<ClassMetadataRecord> userClasses = classes.stream().filter(ClassMetadataRecord::userCode).toList();
        List<ClassMetadataRecord> source = userClasses.isEmpty() ? classes : userClasses;
        List<ClassMetadataRecord> sorted = source.stream().sorted(comparator).collect(Collectors.toList());
        if (sorted.isEmpty()) {
            sorted = classes.stream()
                    .sorted(Comparator
                            .comparing(ClassMetadataRecord::packageName, Comparator.nullsLast(String::compareToIgnoreCase))
                            .thenComparing(ClassMetadataRecord::className, Comparator.nullsLast(String::compareToIgnoreCase)))
                    .collect(Collectors.toList());
        }
        if (limit > 0) {
            return sorted.stream().limit(limit).toList();
        }
        return sorted;
    }

    private Map<String, List<ClassMetadataRecord>> groupClassesByStereotype(List<ClassMetadataRecord> classes) {
        if (classes == null) {
            return Map.of();
        }
        List<ClassMetadataRecord> userClasses = classes.stream().filter(ClassMetadataRecord::userCode).toList();
        List<ClassMetadataRecord> source = userClasses.isEmpty() ? classes : userClasses;
        return source.stream()
                .collect(Collectors.groupingBy(
                        record -> normalizeStereotype(record.stereotype()),
                        LinkedHashMap::new,
                        Collectors.toList()));
    }

    private String componentNodeLabel(String stereotype, List<ClassMetadataRecord> records) {
        if (records == null || records.isEmpty()) {
            return stereotype;
        }
        List<String> names = records.stream()
                .map(ClassMetadataRecord::className)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (names.isEmpty()) {
            return stereotype;
        }
        int limit = Math.min(names.size(), 8);
        List<String> samples = names.subList(0, limit);
        StringBuilder label = new StringBuilder(stereotype)
                .append("\\n")
                .append(String.join("\\n", samples));
        if (names.size() > limit) {
            label.append("\\n… +").append(names.size() - limit).append(" more");
        }
        label.append("\\n(").append(names.size()).append(" classes)");
        return label.toString();
    }

    private DiagramDefinition buildUseCaseDiagram(List<ApiEndpointRecord> endpoints) {
        if (endpoints == null || endpoints.isEmpty()) {
            return null;
        }
        List<ApiEndpointRecord> selected = endpoints.stream()
                .limit(MAX_ENDPOINTS * 2L)
                .toList();
        StringBuilder plantuml = new StringBuilder("@startuml\n");
        plantuml.append("left to right direction\n");
        plantuml.append("actor \"API Client\" as ApiClient\n");
        plantuml.append("actor \"Partner System\" as Partner\n");
        int counter = 1;
        for (ApiEndpointRecord endpoint : selected) {
            String actor = isExternalProtocol(endpoint.protocol()) ? "Partner" : "ApiClient";
            String useCaseName = sanitizeLabel(endpoint.httpMethod(), endpoint.pathOrOperation());
            plantuml.append("(")
                    .append(useCaseName)
                    .append(") as UC")
                    .append(counter)
                    .append("\n");
            plantuml.append(actor)
                    .append(" --> UC")
                    .append(counter)
                    .append("\n");
            counter++;
        }
        plantuml.append("@enduml");

        StringBuilder mermaid = new StringBuilder("flowchart TB\n");
        mermaid.append("    ApiClient([API Client])\n");
        mermaid.append("    Partner([Partner System])\n");
        counter = 1;
        for (ApiEndpointRecord endpoint : selected) {
            String actor = isExternalProtocol(endpoint.protocol()) ? "Partner" : "ApiClient";
            String useCaseName = sanitizeLabel(endpoint.httpMethod(), endpoint.pathOrOperation());
            String node = "UC" + counter;
            mermaid.append("    ")
                    .append(node)
                    .append("[")
                    .append(useCaseName)
                    .append("]\n");
            mermaid.append("    ")
                    .append(actor)
                    .append(" --> ")
                    .append(node)
                    .append("\n");
            counter++;
        }

        return new DiagramDefinition(
                DiagramType.USE_CASE,
                "Endpoint Use Cases",
                plantuml.toString(),
                mermaid.toString(),
                Map.of("endpointCount", selected.size()));
    }

    private DiagramDefinition buildErdDiagram(DbAnalysisResult dbAnalysisResult) {
        if (dbAnalysisResult == null || dbAnalysisResult.entities().isEmpty()) {
            return null;
        }
        List<DbEntityRecord> entities = dbAnalysisResult.entities();
        StringBuilder plantuml = new StringBuilder("@startuml\n");
        entities.forEach(entity -> {
            plantuml.append("entity ")
                    .append(entity.className())
                    .append(" {\n");
            entity.fields().forEach(field -> plantuml
                    .append("  ")
                    .append(field.name())
                    .append(" : ")
                    .append(field.type())
                    .append("\n"));
            plantuml.append("}\n");
        });
        entities.forEach(entity -> entity.relationships().forEach(rel -> plantuml
                .append(entity.className())
                .append(" }o--|| ")
                .append(rel.targetType() == null ? "Unknown" : rel.targetType())
                .append(" : ")
                .append(rel.relationshipType() == null ? "" : rel.relationshipType())
                .append("\n")));
        plantuml.append("@enduml");

        StringBuilder mermaid = new StringBuilder("erDiagram\n");
        entities.forEach(entity -> {
            mermaid.append("    ")
                    .append(entity.className())
                    .append(" {\n");
            entity.fields().forEach(field -> mermaid
                    .append("        ")
                    .append("string ")
                    .append(field.name())
                    .append("\n"));
            mermaid.append("    }\n");
        });
        entities.forEach(entity -> entity.relationships().forEach(rel -> mermaid
                .append("    ")
                .append(entity.className())
                .append(" }o--|| ")
                .append(rel.targetType() == null ? "Unknown" : rel.targetType())
                .append(" : ")
                .append(rel.relationshipType() == null ? "" : rel.relationshipType())
                .append("\n")));

        return new DiagramDefinition(
                DiagramType.ERD,
                "Entity Relationship Diagram",
                plantuml.toString(),
                mermaid.toString(),
                Map.of("entityCount", entities.size()));
    }

    private DiagramDefinition buildDbSchemaDiagram(DbAnalysisResult dbAnalysisResult) {
        if (dbAnalysisResult == null
                || (dbAnalysisResult.operationsByClass().isEmpty() && dbAnalysisResult.entities().isEmpty())) {
            return null;
        }
        StringBuilder plantuml = new StringBuilder("@startuml\n");
        plantuml.append("database DB\n");
        dbAnalysisResult.operationsByClass().forEach((daoClass, ops) -> {
            String alias = daoClass.replace('.', '_');
            plantuml.append("component \"")
                    .append(daoClass)
                    .append("\" as ")
                    .append(alias)
                    .append("\n");
            plantuml.append(alias)
                    .append(" --> DB : ")
                    .append(ops == null ? "Operations" : ops.size() + " ops")
                    .append("\n");
        });
        plantuml.append("@enduml");

        StringBuilder mermaid = new StringBuilder("flowchart LR\n");
        mermaid.append("    DB[(Database)]\n");
        dbAnalysisResult.operationsByClass().forEach((daoClass, ops) -> {
            String alias = daoClass.replace('.', '_');
            mermaid.append("    ")
                    .append(alias)
                    .append("[[")
                    .append(daoClass)
                    .append("]]\n");
            mermaid.append("    ")
                    .append(alias)
                    .append(" --> DB")
                    .append(":::thin\n");
        });
        mermaid.append("    classDef thin stroke:#92a3ff,stroke-width:1px;\n");

        return new DiagramDefinition(
                DiagramType.DB_SCHEMA,
                "DAO to Database Map",
                plantuml.toString(),
                mermaid.toString(),
                Map.of("daoCount", dbAnalysisResult.operationsByClass().size()));
    }

    private List<DiagramDefinition> buildSequenceDiagrams(
            List<ApiEndpointRecord> endpoints,
            ControlFlowSequenceBuilder sequenceBuilder,
            DbAnalysisResult dbAnalysis,
            boolean includeExternal) {
        if (endpoints == null || endpoints.isEmpty()) {
            return List.of();
        }
        Map<String, List<String>> operationsByClass = dbAnalysis == null ? Map.of() : buildOperationsByClass(dbAnalysis);
        List<DiagramDefinition> definitions = new ArrayList<>();
        for (ApiEndpointRecord endpoint : endpoints) {
            DiagramDefinition definition =
                    buildSequenceDiagramForEndpoint(endpoint, sequenceBuilder, operationsByClass, includeExternal);
            if (definition != null) {
                definitions.add(definition);
            }
        }
        return definitions;
    }

    private Map<String, List<String>> buildOperationsByClass(DbAnalysisResult dbAnalysis) {
        return dbAnalysis.operationsByClass().entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue() == null
                                ? List.of()
                                : entry.getValue().stream()
                                        .map(op -> op.methodName() == null ? "operation" : op.methodName())
                                        .collect(Collectors.toList())));
    }

    private DiagramDefinition buildSequenceDiagramForEndpoint(
            ApiEndpointRecord endpoint,
            ControlFlowSequenceBuilder sequenceBuilder,
            Map<String, List<String>> operationsByClass,
            boolean includeExternal) {
        if (endpoint == null) {
            return null;
        }
        SequenceDiagramSources sources =
                renderSequenceDiagram(endpoint, sequenceBuilder, operationsByClass, includeExternal);
        if (sources == null) {
            return null;
        }
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("protocol", endpoint.protocol());
        metadata.put("httpMethod", endpoint.httpMethod());
        metadata.put("pathOrOperation", endpoint.pathOrOperation());
        metadata.put("controllerClass", endpoint.controllerClass());
        metadata.put("controllerMethod", endpoint.controllerMethod());
        metadata.put("includeExternal", includeExternal);
        return new DiagramDefinition(
                DiagramType.SEQUENCE,
                sequenceTitle(endpoint, includeExternal),
                sources.plantuml(),
                sources.mermaid(),
                metadata);
    }

    private SequenceDiagramSources renderSequenceDiagram(
            ApiEndpointRecord endpoint,
            ControlFlowSequenceBuilder sequenceBuilder,
            Map<String, List<String>> operationsByClass,
            boolean includeExternal) {
        if (endpoint == null) {
            return null;
        }

        StringBuilder plantuml = new StringBuilder("@startuml\n");
        plantuml.append("skinparam shadowing false\n");
        plantuml.append("skinparam sequenceMessageAlign center\n");
        plantuml.append("actor \"API Client\" as ApiClient\n");
        plantuml.append("participant \"Database\" as Database\n");
        if (includeExternal) {
            plantuml.append("participant \"External dependencies\" as ExternalLib\n");
        }
        StringBuilder mermaid = new StringBuilder("sequenceDiagram\n");
        mermaid.append("    actor ApiClient as \"API Client\"\n");
        mermaid.append("    participant Database as \"Database\"\n");
        if (includeExternal) {
            mermaid.append("    participant ExternalLib as \"External dependencies\"\n");
        }

        Map<String, String> aliasByClass = new LinkedHashMap<>();
        Set<String> declaredParticipants = new LinkedHashSet<>(Set.of("ApiClient", "Database"));
        if (includeExternal) {
            declaredParticipants.add("ExternalLib");
        }
        java.util.concurrent.atomic.AtomicInteger aliasCounter = new java.util.concurrent.atomic.AtomicInteger(1);

        plantuml.append("== ")
                .append(formatEndpointLabel(endpoint))
                .append(" ==\n");
        mermaid.append("    %% ")
                .append(formatEndpointLabel(endpoint))
                .append("\n");

        String controller = endpoint.controllerClass();
        String controllerAlias = aliasByClass.get(controller);
        if (controllerAlias == null) {
            controllerAlias = "C" + aliasCounter.getAndIncrement();
            aliasByClass.put(controller, controllerAlias);
        }
        if (declaredParticipants.add(controllerAlias)) {
            plantuml.append("participant \"")
                    .append(simpleName(controller))
                    .append("\" as ")
                    .append(controllerAlias)
                    .append("\n");
            mermaid.append("    participant ")
                    .append(controllerAlias)
                    .append(" as \"")
                    .append(simpleName(controller))
                    .append("\"\n");
        }

        plantuml.append("ApiClient -> ")
                .append(controllerAlias)
                .append(" : ")
                .append(endpoint.httpMethod() == null ? "CALL" : endpoint.httpMethod())
                .append(" ")
                .append(endpoint.pathOrOperation())
                .append("\n");
        mermaid.append("    ApiClient->>")
                .append(controllerAlias)
                .append(": ")
                .append(endpoint.httpMethod() == null ? "CALL" : endpoint.httpMethod())
                .append(" ")
                .append(endpoint.pathOrOperation())
                .append("\n");

        ControlFlowSequenceBuilder.SequenceFlow sequenceFlow = sequenceBuilder.build(endpoint, includeExternal);
        if (sequenceFlow == null || sequenceFlow.root() == null) {
            return null;
        }
        renderFlow(
                sequenceFlow.root(),
                controller,
                plantuml,
                mermaid,
                aliasByClass,
                declaredParticipants,
                operationsByClass,
                includeExternal,
                aliasCounter);

        plantuml.append("@enduml");
        return new SequenceDiagramSources(plantuml.toString(), mermaid.toString());
    }

    private void renderFlow(
            ControlFlowSequenceBuilder.FlowBlock block,
            String currentClass,
            StringBuilder plantuml,
            StringBuilder mermaid,
            Map<String, String> aliasByClass,
            Set<String> declaredParticipants,
            Map<String, List<String>> operationsByClass,
            boolean includeExternal,
            java.util.concurrent.atomic.AtomicInteger aliasCounter) {
        if (block == null) {
            return;
        }
        for (ControlFlowSequenceBuilder.FlowElement element : block.elements()) {
            if (element instanceof ControlFlowSequenceBuilder.CallFlow call) {
                renderCall(call, currentClass, plantuml, mermaid, aliasByClass, declaredParticipants,
                        operationsByClass, aliasCounter);
                if (call.inline() != null) {
                    renderFlow(call.inline(), call.targetClass(), plantuml, mermaid, aliasByClass, declaredParticipants,
                            operationsByClass, includeExternal, aliasCounter);
                }
            } else if (element instanceof ControlFlowSequenceBuilder.DispatchFlow dispatch) {
                renderDispatch(dispatch, currentClass, plantuml, mermaid, aliasByClass, declaredParticipants,
                        operationsByClass, includeExternal, aliasCounter);
            } else if (element instanceof ControlFlowSequenceBuilder.IfFlow ifFlow) {
                renderIf(ifFlow, currentClass, plantuml, mermaid, aliasByClass, declaredParticipants,
                        operationsByClass, includeExternal, aliasCounter);
            } else if (element instanceof ControlFlowSequenceBuilder.LoopFlow loopFlow) {
                renderLoop(loopFlow, currentClass, plantuml, mermaid, aliasByClass, declaredParticipants,
                        operationsByClass, includeExternal, aliasCounter);
            } else if (element instanceof ControlFlowSequenceBuilder.SwitchFlow switchFlow) {
                renderSwitch(switchFlow, currentClass, plantuml, mermaid, aliasByClass, declaredParticipants,
                        operationsByClass, includeExternal, aliasCounter);
            } else if (element instanceof ControlFlowSequenceBuilder.TryFlow tryFlow) {
                renderTry(tryFlow, currentClass, plantuml, mermaid, aliasByClass, declaredParticipants,
                        operationsByClass, includeExternal, aliasCounter);
            } else if (element instanceof ControlFlowSequenceBuilder.GroupFlow groupFlow) {
                renderGroup(groupFlow, currentClass, plantuml, mermaid, aliasByClass, declaredParticipants,
                        operationsByClass, includeExternal, aliasCounter);
            } else if (element instanceof ControlFlowSequenceBuilder.NoteFlow noteFlow) {
                renderNote(noteFlow.message(), currentClass, plantuml, mermaid, aliasByClass, declaredParticipants, aliasCounter);
            } else if (element instanceof ControlFlowSequenceBuilder.ReturnFlow) {
                renderNote("return", currentClass, plantuml, mermaid, aliasByClass, declaredParticipants, aliasCounter);
            } else if (element instanceof ControlFlowSequenceBuilder.ThrowFlow) {
                renderNote("throw", currentClass, plantuml, mermaid, aliasByClass, declaredParticipants, aliasCounter);
            } else if (element instanceof ControlFlowSequenceBuilder.BreakFlow) {
                renderNote("break", currentClass, plantuml, mermaid, aliasByClass, declaredParticipants, aliasCounter);
            } else if (element instanceof ControlFlowSequenceBuilder.ContinueFlow) {
                renderNote("continue", currentClass, plantuml, mermaid, aliasByClass, declaredParticipants, aliasCounter);
            }
        }
    }

    private void renderCall(
            ControlFlowSequenceBuilder.CallFlow call,
            String currentClass,
            StringBuilder plantuml,
            StringBuilder mermaid,
            Map<String, String> aliasByClass,
            Set<String> declaredParticipants,
            Map<String, List<String>> operationsByClass,
            java.util.concurrent.atomic.AtomicInteger aliasCounter) {
        if (call == null) {
            return;
        }
        String sourceClass = call.sourceClass() == null ? currentClass : call.sourceClass();
        String targetClass = call.targetClass();
        if (sourceClass == null || targetClass == null) {
            return;
        }
        String sourceAlias = ensureParticipant(sourceClass, plantuml, mermaid, aliasByClass, declaredParticipants, aliasCounter);
        String targetAlias = ensureParticipant(targetClass, plantuml, mermaid, aliasByClass, declaredParticipants, aliasCounter);
        String label = call.methodName() == null ? "call" : call.methodName() + "()";

        plantuml.append(sourceAlias)
                .append(" -> ")
                .append(targetAlias)
                .append(" : ")
                .append(label)
                .append("\n");
        mermaid.append("    ")
                .append(sourceAlias)
                .append("->>")
                .append(targetAlias)
                .append(": ")
                .append(label)
                .append("\n");

        List<String> daoOps = operationsByClass.get(targetClass);
        if (daoOps != null && !daoOps.isEmpty()) {
            String dbLabel = formatOperationLabel(daoOps);
            plantuml.append(targetAlias)
                    .append(" -> Database : ")
                    .append(dbLabel)
                    .append("\n");
            mermaid.append("    ")
                    .append(targetAlias)
                    .append("->>Database: ")
                    .append(dbLabel)
                    .append("\n");
        }
    }

    private void renderDispatch(
            ControlFlowSequenceBuilder.DispatchFlow dispatch,
            String currentClass,
            StringBuilder plantuml,
            StringBuilder mermaid,
            Map<String, String> aliasByClass,
            Set<String> declaredParticipants,
            Map<String, List<String>> operationsByClass,
            boolean includeExternal,
            java.util.concurrent.atomic.AtomicInteger aliasCounter) {
        if (dispatch.cases().isEmpty()) {
            renderCall(new ControlFlowSequenceBuilder.CallFlow(
                    dispatch.sourceClass(), dispatch.targetClass(), dispatch.methodName(), null),
                    currentClass, plantuml, mermaid, aliasByClass, declaredParticipants, operationsByClass, aliasCounter);
            return;
        }
        boolean started = false;
        for (ControlFlowSequenceBuilder.DispatchCase dispatchCase : dispatch.cases()) {
            String label = "impl " + dispatchCase.label();
            if (!started) {
                plantuml.append("alt ").append(label).append("\n");
                mermaid.append("    alt ").append(label).append("\n");
                started = true;
            } else {
                plantuml.append("else ").append(label).append("\n");
                mermaid.append("    else ").append(label).append("\n");
            }
            ControlFlowSequenceBuilder.CallFlow call = new ControlFlowSequenceBuilder.CallFlow(
                    dispatch.sourceClass(), dispatchCase.label(), dispatch.methodName(), null);
            renderCall(call, currentClass, plantuml, mermaid, aliasByClass, declaredParticipants, operationsByClass, aliasCounter);
            renderFlow(dispatchCase.body(), dispatchCase.label(), plantuml, mermaid, aliasByClass, declaredParticipants,
                    operationsByClass, includeExternal, aliasCounter);
        }
        if (started) {
            plantuml.append("end\n");
            mermaid.append("    end\n");
        }
    }

    private void renderIf(
            ControlFlowSequenceBuilder.IfFlow ifFlow,
            String currentClass,
            StringBuilder plantuml,
            StringBuilder mermaid,
            Map<String, String> aliasByClass,
            Set<String> declaredParticipants,
            Map<String, List<String>> operationsByClass,
            boolean includeExternal,
            java.util.concurrent.atomic.AtomicInteger aliasCounter) {
        boolean hasElse = ifFlow.elseBlock() != null;
        if (hasElse) {
            plantuml.append("alt ").append(ifFlow.condition()).append("\n");
            mermaid.append("    alt ").append(ifFlow.condition()).append("\n");
        } else {
            plantuml.append("opt ").append(ifFlow.condition()).append("\n");
            mermaid.append("    opt ").append(ifFlow.condition()).append("\n");
        }
        renderFlow(ifFlow.thenBlock(), currentClass, plantuml, mermaid, aliasByClass, declaredParticipants,
                operationsByClass, includeExternal, aliasCounter);
        if (hasElse) {
            plantuml.append("else\n");
            mermaid.append("    else\n");
            renderFlow(ifFlow.elseBlock(), currentClass, plantuml, mermaid, aliasByClass, declaredParticipants,
                    operationsByClass, includeExternal, aliasCounter);
        }
        plantuml.append("end\n");
        mermaid.append("    end\n");
    }

    private void renderLoop(
            ControlFlowSequenceBuilder.LoopFlow loopFlow,
            String currentClass,
            StringBuilder plantuml,
            StringBuilder mermaid,
            Map<String, String> aliasByClass,
            Set<String> declaredParticipants,
            Map<String, List<String>> operationsByClass,
            boolean includeExternal,
            java.util.concurrent.atomic.AtomicInteger aliasCounter) {
        plantuml.append("loop ").append(loopFlow.condition()).append("\n");
        mermaid.append("    loop ").append(loopFlow.condition()).append("\n");
        renderFlow(loopFlow.body(), currentClass, plantuml, mermaid, aliasByClass, declaredParticipants,
                operationsByClass, includeExternal, aliasCounter);
        plantuml.append("end\n");
        mermaid.append("    end\n");
    }

    private void renderSwitch(
            ControlFlowSequenceBuilder.SwitchFlow switchFlow,
            String currentClass,
            StringBuilder plantuml,
            StringBuilder mermaid,
            Map<String, String> aliasByClass,
            Set<String> declaredParticipants,
            Map<String, List<String>> operationsByClass,
            boolean includeExternal,
            java.util.concurrent.atomic.AtomicInteger aliasCounter) {
        boolean started = false;
        for (ControlFlowSequenceBuilder.CaseFlow caseFlow : switchFlow.cases()) {
            String label = switchFlow.selector() + " == " + caseFlow.label();
            if (!started) {
                plantuml.append("alt ").append(label).append("\n");
                mermaid.append("    alt ").append(label).append("\n");
                started = true;
            } else {
                plantuml.append("else ").append(label).append("\n");
                mermaid.append("    else ").append(label).append("\n");
            }
            renderFlow(caseFlow.body(), currentClass, plantuml, mermaid, aliasByClass, declaredParticipants,
                    operationsByClass, includeExternal, aliasCounter);
        }
        if (started) {
            plantuml.append("end\n");
            mermaid.append("    end\n");
        }
    }

    private void renderTry(
            ControlFlowSequenceBuilder.TryFlow tryFlow,
            String currentClass,
            StringBuilder plantuml,
            StringBuilder mermaid,
            Map<String, String> aliasByClass,
            Set<String> declaredParticipants,
            Map<String, List<String>> operationsByClass,
            boolean includeExternal,
            java.util.concurrent.atomic.AtomicInteger aliasCounter) {
        plantuml.append("group try\n");
        mermaid.append("    alt try\n");
        renderFlow(tryFlow.tryBlock(), currentClass, plantuml, mermaid, aliasByClass, declaredParticipants,
                operationsByClass, includeExternal, aliasCounter);
        if (tryFlow.catches().isEmpty()) {
            plantuml.append("end\n");
            mermaid.append("    end\n");
        } else {
            for (ControlFlowSequenceBuilder.CatchFlow catchFlow : tryFlow.catches()) {
                plantuml.append("else catch ").append(catchFlow.label()).append("\n");
                mermaid.append("    else catch ").append(catchFlow.label()).append("\n");
                renderFlow(catchFlow.body(), currentClass, plantuml, mermaid, aliasByClass, declaredParticipants,
                        operationsByClass, includeExternal, aliasCounter);
            }
            plantuml.append("end\n");
            mermaid.append("    end\n");
        }
        if (tryFlow.finallyBlock() != null) {
            plantuml.append("opt finally (always)\n");
            mermaid.append("    opt finally (always)\n");
            renderFlow(tryFlow.finallyBlock(), currentClass, plantuml, mermaid, aliasByClass, declaredParticipants,
                    operationsByClass, includeExternal, aliasCounter);
            plantuml.append("end\n");
            mermaid.append("    end\n");
        }
    }

    private void renderGroup(
            ControlFlowSequenceBuilder.GroupFlow groupFlow,
            String currentClass,
            StringBuilder plantuml,
            StringBuilder mermaid,
            Map<String, String> aliasByClass,
            Set<String> declaredParticipants,
            Map<String, List<String>> operationsByClass,
            boolean includeExternal,
            java.util.concurrent.atomic.AtomicInteger aliasCounter) {
        plantuml.append("group ").append(groupFlow.label()).append("\n");
        mermaid.append("    opt ").append(groupFlow.label()).append("\n");
        renderFlow(groupFlow.body(), currentClass, plantuml, mermaid, aliasByClass, declaredParticipants,
                operationsByClass, includeExternal, aliasCounter);
        plantuml.append("end\n");
        mermaid.append("    end\n");
    }

    private void renderNote(
            String message,
            String currentClass,
            StringBuilder plantuml,
            StringBuilder mermaid,
            Map<String, String> aliasByClass,
            Set<String> declaredParticipants,
            java.util.concurrent.atomic.AtomicInteger aliasCounter) {
        if (message == null || message.isBlank()) {
            return;
        }
        String targetAlias = ensureParticipant(
                currentClass == null ? "Unknown" : currentClass,
                plantuml,
                mermaid,
                aliasByClass,
                declaredParticipants,
                aliasCounter);
        plantuml.append("note over ")
                .append(targetAlias)
                .append(" : ")
                .append(message)
                .append("\n");
        mermaid.append("    Note over ")
                .append(targetAlias)
                .append(": ")
                .append(message)
                .append("\n");
    }

    private String ensureParticipant(
            String className,
            StringBuilder plantuml,
            StringBuilder mermaid,
            Map<String, String> aliasByClass,
            Set<String> declaredParticipants,
            java.util.concurrent.atomic.AtomicInteger aliasCounter) {
        if (className == null || className.isBlank()) {
            return "Unknown";
        }
        String alias = aliasByClass.get(className);
        if (alias == null) {
            alias = "N" + aliasCounter.getAndIncrement();
            aliasByClass.put(className, alias);
        }
        if (declaredParticipants.add(alias)) {
            String label = simpleName(className);
            plantuml.append("participant \"")
                    .append(label)
                    .append("\" as ")
                    .append(alias)
                    .append("\n");
            mermaid.append("    participant ")
                    .append(alias)
                    .append(" as \"")
                    .append(label)
                    .append("\"\n");
        }
        return alias;
    }

    private String sequenceTitle(ApiEndpointRecord endpoint, boolean includeExternal) {
        String label = formatEndpointLabel(endpoint);
        return includeExternal ? "Call Flow (External dependencies) – " + label : "Call Flow – " + label;
    }

    private String formatEndpointLabel(ApiEndpointRecord endpoint) {
        if (endpoint == null) {
            return "Endpoint";
        }
        String method = endpoint.httpMethod();
        if (method == null || method.isBlank()) {
            method = endpoint.protocol();
        }
        String path = endpoint.pathOrOperation();
        if (method != null && !method.isBlank() && path != null && !path.isBlank()) {
            return (method + " " + path).trim();
        }
        if (path != null && !path.isBlank()) {
            return path;
        }
        if (method != null && !method.isBlank()) {
            return method;
        }
        if (endpoint.controllerMethod() != null && !endpoint.controllerMethod().isBlank()) {
            return endpoint.controllerMethod();
        }
        return "Endpoint";
    }

    private Map<String, List<String>> buildCallFlows(
            List<ApiEndpointRecord> endpoints, ControlFlowSequenceBuilder sequenceBuilder, int limit) {
        if (endpoints == null || sequenceBuilder == null) {
            return Map.of();
        }
        Map<String, List<String>> flows = new LinkedHashMap<>();
        endpoints.stream()
                .limit(Math.max(limit, 0))
                .forEach(endpoint -> {
                    ControlFlowSequenceBuilder.SequenceFlow sequenceFlow = sequenceBuilder.build(endpoint, false);
                    if (sequenceFlow == null) {
                        return;
                    }
                    List<String> flow = sequenceBuilder.summarize(sequenceFlow, Math.max(limit, 0));
                    if (!flow.isEmpty()) {
                        flows.put(formatEndpointLabel(endpoint), flow);
                    }
                });
        return flows;
    }

    private String normalizeStereotype(String stereotype) {
        if (stereotype == null || stereotype.isBlank()) {
            return "OTHER";
        }
        return stereotype.toUpperCase(Locale.ROOT);
    }

    private String findStereotype(String fqn, List<ClassMetadataRecord> classes) {
        if (fqn == null) {
            return null;
        }
        return classes.stream()
                .filter(record -> fqn.equals(record.fullyQualifiedName()))
                .map(record -> normalizeStereotype(record.stereotype()))
                .findFirst()
                .orElse(null);
    }

    private Map<String, String> buildAliases(List<String> fullyQualifiedNames) {
        Map<String, String> aliases = new LinkedHashMap<>();
        int counter = 1;
        for (String fqn : fullyQualifiedNames) {
            aliases.put(fqn, "C" + counter++);
        }
        return aliases;
    }

    private boolean isExternalProtocol(String protocol) {
        if (protocol == null) {
            return false;
        }
        String normalized = protocol.toUpperCase(Locale.ROOT);
        return normalized.contains("SOAP") || normalized.contains("LEGACY");
    }

    private String sanitizeLabel(String method, String pathOrOperation) {
        String normalized = (method == null ? "" : method + " ") + (pathOrOperation == null ? "" : pathOrOperation);
        return normalized.trim().isEmpty() ? "Endpoint" : normalized.trim();
    }

    private String sanitizeMethodName(String methodName) {
        if (methodName == null) {
            return null;
        }
        String trimmed = methodName.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String simpleName(String fqn) {
        if (fqn == null) {
            return "";
        }
        int idx = fqn.lastIndexOf('.');
        return idx >= 0 ? fqn.substring(idx + 1) : fqn;
    }

    private String formatOperationLabel(List<String> operations) {
        if (operations == null || operations.isEmpty()) {
            return "DAO ops";
        }
        String joined = operations.stream()
                .map(op -> op.endsWith("()") ? op : op + "()")
                .limit(3)
                .collect(Collectors.joining(", "));
        if (operations.size() > 3) {
            return joined + " +" + (operations.size() - 3) + " more";
        }
        return joined;
    }

    private record Edge(String from, String to) {}

    private record SequenceDiagramSources(String plantuml, String mermaid) {}

    private record ModuleKey(String key, Path relativePath, int depth) {}

    private record ModuleMapping(
            Map<String, List<ClassMetadataRecord>> classesByModule,
            Map<String, String> classToModule,
            List<String> moduleKeys) {

        private static ModuleMapping empty() {
            return new ModuleMapping(Map.of(), Map.of(), List.of());
        }
    }

}
