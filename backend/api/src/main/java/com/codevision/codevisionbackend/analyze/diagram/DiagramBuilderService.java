package com.codevision.codevisionbackend.analyze.diagram;

import com.codevision.codevisionbackend.analyze.diagram.CallGraph.GraphNode;
import com.codevision.codevisionbackend.analyze.scanner.ApiEndpointRecord;
import com.codevision.codevisionbackend.analyze.scanner.ClassMetadataRecord;
import com.codevision.codevisionbackend.analyze.scanner.DbAnalysisResult;
import com.codevision.codevisionbackend.analyze.scanner.DbEntityRecord;
import com.codevision.codevisionbackend.project.diagram.DiagramType;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
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
    private static final int MAX_SEQUENCE_DEPTH = 4;
    private static final int MAX_SEQUENCE_STEPS = 42;

    private final CallGraphBuilder callGraphBuilder;

    public DiagramBuilderService(CallGraphBuilder callGraphBuilder) {
        this.callGraphBuilder = callGraphBuilder;
    }

    public DiagramGenerationResult generate(
            Path repoRoot,
            List<ClassMetadataRecord> classRecords,
            List<ApiEndpointRecord> apiEndpoints,
            DbAnalysisResult dbAnalysisResult) {
        CallGraph graph = callGraphBuilder.build(repoRoot, classRecords);
        List<ApiEndpointRecord> limitedEndpoints = selectEndpoints(apiEndpoints, MAX_ENDPOINTS);
        List<DiagramDefinition> diagrams = new ArrayList<>();
        Map<String, List<String>> callFlows = buildCallFlows(apiEndpoints, graph, MAX_ENDPOINTS * 2);

        Optional.ofNullable(buildClassDiagram(classRecords, graph)).ifPresent(diagrams::add);
        Optional.ofNullable(buildComponentDiagram(classRecords, graph)).ifPresent(diagrams::add);
        Optional.ofNullable(buildUseCaseDiagram(apiEndpoints)).ifPresent(diagrams::add);
        Optional.ofNullable(buildErdDiagram(dbAnalysisResult)).ifPresent(diagrams::add);
        Optional.ofNullable(buildDbSchemaDiagram(dbAnalysisResult)).ifPresent(diagrams::add);
        diagrams.addAll(buildSequenceDiagrams(limitedEndpoints, graph, dbAnalysisResult, false));
        diagrams.addAll(buildSequenceDiagrams(limitedEndpoints, graph, dbAnalysisResult, true));

        return new DiagramGenerationResult(diagrams, callFlows);
    }

    private DiagramDefinition buildClassDiagram(List<ClassMetadataRecord> classes, CallGraph graph) {
        if (classes == null || classes.isEmpty()) {
            return null;
        }
        List<ClassMetadataRecord> selected = selectClassesForDiagram(classes, graph);
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
                DiagramType.CLASS, "Core Class Relationships", plantuml.toString(), mermaid.toString(), Map.of());
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

    private List<ApiEndpointRecord> selectEndpoints(List<ApiEndpointRecord> endpoints, int limit) {
        if (endpoints == null || endpoints.isEmpty()) {
            return List.of();
        }
        return endpoints.stream().limit(Math.max(limit, 0)).toList();
    }

    private List<ClassMetadataRecord> selectClassesForDiagram(List<ClassMetadataRecord> classes, CallGraph graph) {
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
        return sorted.stream().limit(MAX_CLASS_NODES).toList();
    }

    private Map<String, List<ClassMetadataRecord>> groupClassesByStereotype(List<ClassMetadataRecord> classes) {
        if (classes == null || classes.isEmpty()) {
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
        List<String> samples = records.stream()
                .map(ClassMetadataRecord::className)
                .filter(Objects::nonNull)
                .distinct()
                .limit(3)
                .toList();
        if (samples.isEmpty()) {
            return stereotype;
        }
        return stereotype + "\\n" + String.join("\\n", samples);
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

    private List<ClassMetadataRecord> selectClassesForDiagram(List<ClassMetadataRecord> classes, CallGraph graph) {
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
        return sorted.stream().limit(MAX_CLASS_NODES).toList();
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
        List<String> samples = records.stream()
                .map(ClassMetadataRecord::className)
                .filter(Objects::nonNull)
                .distinct()
                .limit(3)
                .toList();
        if (samples.isEmpty()) {
            return stereotype;
        }
        return stereotype + "\\n" + String.join("\\n", samples);
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
            CallGraph graph,
            DbAnalysisResult dbAnalysis,
            boolean includeExternal) {
        if (endpoints == null || endpoints.isEmpty()) {
            return List.of();
        }
        Map<String, List<String>> operationsByClass = dbAnalysis == null ? Map.of() : buildOperationsByClass(dbAnalysis);
        List<DiagramDefinition> definitions = new ArrayList<>();
        for (ApiEndpointRecord endpoint : endpoints) {
            DiagramDefinition definition = buildSequenceDiagramForEndpoint(endpoint, graph, operationsByClass, includeExternal);
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
            CallGraph graph,
            Map<String, List<String>> operationsByClass,
            boolean includeExternal) {
        if (endpoint == null) {
            return null;
        }
        String controller = endpoint.controllerClass();
        if (controller == null || !graph.nodes().containsKey(controller)) {
            return null;
        }

        SequenceDiagramSources sources =
                renderSequenceDiagram(endpoint, graph, operationsByClass, includeExternal);
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
            CallGraph graph,
            Map<String, List<String>> operationsByClass,
            boolean includeExternal) {
        String controller = endpoint.controllerClass();
        if (controller == null || !graph.nodes().containsKey(controller)) {
            return null;
        }

        StringBuilder plantuml = new StringBuilder("@startuml\n");
        plantuml.append("skinparam shadowing false\n");
        plantuml.append("skinparam sequenceMessageAlign center\n");
        plantuml.append("actor \"API Client\" as ApiClient\n");
        plantuml.append("participant \"Database\" as Database\n");
        if (includeExternal) {
            plantuml.append("participant \"External codeviz2\" as ExternalLib\n");
        }
        StringBuilder mermaid = new StringBuilder("sequenceDiagram\n");
        mermaid.append("    actor ApiClient as \"API Client\"\n");
        mermaid.append("    participant Database as \"Database\"\n");
        if (includeExternal) {
            mermaid.append("    participant ExternalLib as \"External codeviz2\"\n");
        }

        Map<String, String> aliasByClass = new LinkedHashMap<>();
        Set<String> declaredParticipants = new LinkedHashSet<>(Set.of("ApiClient", "Database"));
        if (includeExternal) {
            declaredParticipants.add("ExternalLib");
        }
        int aliasCounter = 1;

        plantuml.append("== ")
                .append(formatEndpointLabel(endpoint))
                .append(" ==\n");
        mermaid.append("    %% ")
                .append(formatEndpointLabel(endpoint))
                .append("\n");

        String controllerAlias = aliasByClass.get(controller);
        if (controllerAlias == null) {
            controllerAlias = "C" + aliasCounter++;
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

        List<SequenceStep> steps = buildSequenceSteps(controller, graph, includeExternal);
        int stepBudget = MAX_SEQUENCE_STEPS;
        for (SequenceStep step : steps) {
            if (stepBudget-- <= 0) {
                break;
            }
            String sourceAlias = aliasByClass.get(step.source());
            if (sourceAlias == null) {
                sourceAlias = "N" + aliasCounter++;
                aliasByClass.put(step.source(), sourceAlias);
            }
            if (declaredParticipants.add(sourceAlias)) {
                plantuml.append("participant \"")
                        .append(step.sourceLabel())
                        .append("\" as ")
                        .append(sourceAlias)
                        .append("\n");
                mermaid.append("    participant ")
                        .append(sourceAlias)
                        .append(" as \"")
                        .append(step.sourceLabel())
                        .append("\"\n");
            }

            String targetAlias = aliasByClass.get(step.target());
            if (targetAlias == null) {
                targetAlias = "N" + aliasCounter++;
                aliasByClass.put(step.target(), targetAlias);
            }
            if (declaredParticipants.add(targetAlias)) {
                plantuml.append("participant \"")
                        .append(step.targetLabel())
                        .append("\" as ")
                        .append(targetAlias)
                        .append("\n");
                mermaid.append("    participant ")
                        .append(targetAlias)
                        .append(" as \"")
                        .append(step.targetLabel())
                        .append("\"\n");
            }

            plantuml.append(sourceAlias)
                    .append(" -> ")
                    .append(targetAlias)
                    .append(" : ")
                    .append(step.message())
                    .append("\n");
            mermaid.append("    ")
                    .append(sourceAlias)
                    .append("->>")
                    .append(targetAlias)
                    .append(": ")
                    .append(step.message())
                    .append("\n");

            if (operationsByClass.containsKey(step.target())) {
                plantuml.append(targetAlias)
                        .append(" -> Database : ")
                        .append(operationsByClass.get(step.target()).size())
                        .append(" ops\n");
                mermaid.append("    ")
                        .append(targetAlias)
                        .append("->>Database: DAO ops\n");
            }
        }

        plantuml.append("@enduml");
        return new SequenceDiagramSources(plantuml.toString(), mermaid.toString());
    }

    private String sequenceTitle(ApiEndpointRecord endpoint, boolean includeExternal) {
        String label = formatEndpointLabel(endpoint);
        return includeExternal ? "Call Flow (External) – " + label : "Call Flow – " + label;
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

    private List<SequenceStep> buildSequenceSteps(String start, CallGraph graph, boolean includeExternal) {
        List<SequenceStep> steps = new ArrayList<>();
        if (!graph.nodes().containsKey(start)) {
            return steps;
        }
        Deque<String> stack = new ArrayDeque<>();
        traverseSequence(start, graph, includeExternal, stack, steps, 0);
        return steps;
    }

    private void traverseSequence(
            String current,
            CallGraph graph,
            boolean includeExternal,
            Deque<String> stack,
            List<SequenceStep> steps,
            int depth) {
        if (depth >= MAX_SEQUENCE_DEPTH) {
            return;
        }
        stack.push(current);
        for (String target : graph.targets(current)) {
            GraphNode node = graph.nodes().get(target);
            if (node == null) {
                continue;
            }
            if (!includeExternal && node.external()) {
                continue;
            }
            if (stack.contains(target)) {
                steps.add(SequenceStep.cycle(current, target, node));
                continue;
            }
            steps.add(SequenceStep.call(current, target, node));
            traverseSequence(target, graph, includeExternal, stack, steps, depth + 1);
        }
        stack.pop();
    }

    private Map<String, List<String>> buildCallFlows(List<ApiEndpointRecord> endpoints, CallGraph graph, int limit) {
        if (endpoints == null) {
            return Map.of();
        }
        Map<String, List<String>> flows = new LinkedHashMap<>();
        endpoints.stream()
                .limit(Math.max(limit, 0))
                .forEach(endpoint -> {
                    String controller = endpoint.controllerClass();
                    if (controller == null || !graph.nodes().containsKey(controller)) {
                        return;
                    }
                    List<String> flow = buildSequenceSteps(controller, graph, false).stream()
                            .map(step -> step.sourceLabel() + " -> " + step.targetLabel())
                            .limit(12)
                            .collect(Collectors.toList());
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

    private String simpleName(String fqn) {
        if (fqn == null) {
            return "";
        }
        int idx = fqn.lastIndexOf('.');
        return idx >= 0 ? fqn.substring(idx + 1) : fqn;
    }

    private record Edge(String from, String to) {}

    private record SequenceDiagramSources(String plantuml, String mermaid) {}

    private static class SequenceStep {
        private final String source;
        private final String target;
        private final String message;
        private final String sourceLabel;
        private final String targetLabel;

        private SequenceStep(
                String source, String target, String message, String sourceLabel, String targetLabel) {
            this.source = source;
            this.target = target;
            this.message = message;
            this.sourceLabel = sourceLabel;
            this.targetLabel = targetLabel;
        }

        static SequenceStep call(String source, String target, GraphNode targetNode) {
            return new SequenceStep(
                    source,
                    target,
                    "call",
                    simple(source),
                    targetNode == null ? simple(target) : targetNode.simpleName());
        }

        static SequenceStep cycle(String source, String target, GraphNode targetNode) {
            return new SequenceStep(
                    source,
                    target,
                    "[cycle -> " + simple(target) + "]",
                    simple(source),
                    targetNode == null ? simple(target) : targetNode.simpleName());
        }

        private static String simple(String value) {
            if (value == null) {
                return "";
            }
            int idx = value.lastIndexOf('.');
            return idx >= 0 ? value.substring(idx + 1) : value;
        }

        public String source() {
            return source;
        }

        public String target() {
            return target;
        }

        public String message() {
            return message;
        }

        public String sourceLabel() {
            return sourceLabel;
        }

        public String targetLabel() {
            return targetLabel;
        }
    }
}
