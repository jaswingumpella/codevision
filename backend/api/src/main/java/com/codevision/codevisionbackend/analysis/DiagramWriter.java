package com.codevision.codevisionbackend.analysis;

import com.codevision.codevisionbackend.analysis.GraphModel.ClassNode;
import com.codevision.codevisionbackend.analysis.GraphModel.DependencyEdge;
import com.codevision.codevisionbackend.analysis.GraphModel.DependencyKind;
import com.codevision.codevisionbackend.analysis.GraphModel.EndpointNode;
import com.codevision.codevisionbackend.analysis.GraphModel.EndpointType;
import com.codevision.codevisionbackend.analysis.GraphModel.FieldModel;
import com.codevision.codevisionbackend.analysis.GraphModel.SequenceNode;
import com.codevision.codevisionbackend.analysis.config.CompiledAnalysisProperties;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Produces PlantUML and Mermaid diagrams from the compiled graph, ensuring cycle-safe sequence
 * traversal.
 */
@Component
public class DiagramWriter {

    private static final Logger log = LoggerFactory.getLogger(DiagramWriter.class);
    private static final Pattern NON_ALPHANUM = Pattern.compile("[^a-zA-Z0-9]");

    public DiagramArtifacts writeDiagrams(GraphModel model, Path outputDir, CompiledAnalysisProperties properties)
            throws IOException {
        Files.createDirectories(outputDir);
        Path classDiagram = outputDir.resolve("class-diagram.puml");
        Path erdPuml = outputDir.resolve("erd.puml");
        Path erdMermaid = outputDir.resolve("erd.mmd");
        Files.writeString(classDiagram, buildClassDiagram(model), StandardCharsets.UTF_8);
        Files.writeString(erdPuml, buildErdPlantUml(model), StandardCharsets.UTF_8);
        Files.writeString(erdMermaid, buildErdMermaid(model), StandardCharsets.UTF_8);
        List<Path> sequences = writeSequenceDiagrams(model, outputDir, properties.getMaxCallDepth());
        return new DiagramArtifacts(classDiagram, erdPuml, erdMermaid, sequences);
    }

    private String buildClassDiagram(GraphModel model) {
        StringBuilder builder = new StringBuilder();
        builder.append("@startuml\n");
        builder.append("set namespaceSeparator .\n");
        for (ClassNode node : model.sortedClasses()) {
            builder.append("class ").append(node.getName());
            List<String> stereotypes = new ArrayList<>(node.getStereotypes());
            if (node.isInCycle()) {
                stereotypes.add("CYCLE");
            }
            if (!stereotypes.isEmpty()) {
                builder.append(" <<").append(String.join(", ", stereotypes)).append(">>");
            }
            builder.append(" {\n");
            for (FieldModel field : node.getFields()) {
                builder.append("  ")
                        .append(field.isInjected() ? "*" : "")
                        .append(field.getName())
                        .append(" : ")
                        .append(field.getType())
                        .append("\n");
            }
            builder.append("}\n");
        }

        for (DependencyEdge edge : model.getDependencyEdges()) {
            if (edge.getFromClass() == null || edge.getToClass() == null) {
                continue;
            }
            builder.append(alias(edge.getFromClass()));
            switch (edge.getKind()) {
                case EXTENDS -> builder.append(" <|-- ");
                case IMPLEMENTS -> builder.append(" <|.. ");
                case CALL, INJECTION, FIELD -> builder.append(" --> ");
                default -> builder.append(" ..> ");
            }
            builder.append(alias(edge.getToClass()));
            if (edge.getLabel() != null) {
                builder.append(" : ").append(edge.getLabel());
            }
            builder.append("\n");
        }
        builder.append("@enduml\n");
        return builder.toString();
    }

    private String buildErdPlantUml(GraphModel model) {
        StringBuilder builder = new StringBuilder();
        builder.append("@startuml\n");
        builder.append("hide circle\n");
        builder.append("skinparam linetype ortho\n");
        List<ClassNode> entities = model.sortedClasses().stream()
                .filter(ClassNode::isEntity)
                .toList();
        for (ClassNode entity : entities) {
            builder.append("entity ").append(alias(entity.getName())).append(" {\n");
            for (FieldModel field : entity.getFields()) {
                builder.append("  ")
                        .append(field.isRelationship() ? "ref " : "")
                        .append(field.getName())
                        .append(" : ")
                        .append(field.getType())
                        .append("\n");
            }
            builder.append("}\n");
        }
        for (ClassNode entity : entities) {
            for (FieldModel field : entity.getFields()) {
                if (!field.isRelationship()) {
                    continue;
                }
                String targetType = normalizeType(field.getType());
                if (targetType == null) {
                    continue;
                }
                builder.append(alias(entity.getName()))
                        .append(" --> ")
                        .append(alias(targetType))
                        .append(" : ")
                        .append(field.getName())
                        .append("\n");
            }
        }
        builder.append("@enduml\n");
        return builder.toString();
    }

    private String buildErdMermaid(GraphModel model) {
        StringBuilder builder = new StringBuilder();
        builder.append("erDiagram\n");
        List<ClassNode> entities = model.sortedClasses().stream()
                .filter(ClassNode::isEntity)
                .toList();
        for (ClassNode entity : entities) {
            builder.append("  ")
                    .append(alias(entity.getName()))
                    .append(" {\n");
            for (FieldModel field : entity.getFields()) {
                builder.append("    ")
                        .append(field.getType())
                        .append(" ")
                        .append(field.getName())
                        .append("\n");
            }
            builder.append("  }\n");
        }
        for (ClassNode entity : entities) {
            for (FieldModel field : entity.getFields()) {
                if (!field.isRelationship()) {
                    continue;
                }
                String targetType = normalizeType(field.getType());
                if (targetType == null) {
                    continue;
                }
                builder.append("  ")
                        .append(alias(entity.getName()))
                        .append(" ||--o{ ")
                        .append(alias(targetType))
                        .append(" : ")
                        .append(field.getName())
                        .append("\n");
            }
        }
        return builder.toString();
    }

    private List<Path> writeSequenceDiagrams(GraphModel model, Path outputDir, int maxDepth) throws IOException {
        Map<String, Set<String>> adjacency = model.buildCallAdjacency();
        List<Path> written = new ArrayList<>();
        List<EndpointNode> httpEndpoints = model.getEndpoints().stream()
                .filter(endpoint -> endpoint.getType() == EndpointType.HTTP)
                .sorted(Comparator.comparing(EndpointNode::getControllerClass, Comparator.nullsLast(
                                String.CASE_INSENSITIVE_ORDER))
                        .thenComparing(EndpointNode::getPath, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();
        int limit = Math.min(httpEndpoints.size(), 25);
        for (int i = 0; i < limit; i++) {
            EndpointNode endpoint = httpEndpoints.get(i);
            String filename = "seq_%02d_%s.puml".formatted(
                    i + 1,
                    sanitize(endpoint.getControllerClass() + "_" + endpoint.getControllerMethod()));
            Path path = outputDir.resolve(filename);
            Files.writeString(path, buildSequenceDiagram(endpoint, adjacency, model, maxDepth), StandardCharsets.UTF_8);
            written.add(path);
        }
        return written;
    }

    private String buildSequenceDiagram(
            EndpointNode endpoint,
            Map<String, Set<String>> adjacency,
            GraphModel model,
            int maxDepth) {
        StringBuilder builder = new StringBuilder();
        builder.append("@startuml\n");
        builder.append("actor Client\n");
        String controllerAlias = alias(endpoint.getControllerClass());
        builder.append("Client -> ")
                .append(controllerAlias)
                .append(" : ")
                .append(endpoint.getHttpMethod() != null ? endpoint.getHttpMethod() : "ANY")
                .append(" ")
                .append(endpoint.getPath())
                .append("\n");
        traverseSequence(endpoint.getControllerClass(), adjacency, model, maxDepth, new ArrayDeque<>(), builder);
        builder.append("@enduml\n");
        return builder.toString();
    }

    private void traverseSequence(
            String className,
            Map<String, Set<String>> adjacency,
            GraphModel model,
            int depth,
            Deque<String> stack,
            StringBuilder builder) {
        if (depth <= 0) {
            return;
        }
        stack.push(className);
        for (String callee : adjacency.getOrDefault(className, Set.of())) {
            if (stack.contains(callee)) {
                builder.append("loop Cyclic dependency (SCC ")
                        .append(model.getClasses().getOrDefault(callee, new ClassNode()).getSccId())
                        .append(")\n");
                builder.append(alias(className)).append(" -> ").append(alias(callee)).append(" : call\n");
                builder.append("end\n");
                continue;
            }
            builder.append(alias(className)).append(" -> ").append(alias(callee)).append(" : call\n");
            traverseSequence(callee, adjacency, model, depth - 1, stack, builder);
        }
        stack.pop();
    }

    private String alias(String fqcn) {
        if (fqcn == null) {
            return "Unknown";
        }
        return fqcn;
    }

    private String normalizeType(String rawType) {
        if (rawType == null) {
            return null;
        }
        String type = rawType;
        int generics = type.indexOf('<');
        if (generics >= 0) {
            type = type.substring(0, generics);
        }
        type = type.replace('/', '.').replace("L", "").trim();
        return type.contains(".") ? type : null;
    }

    private String sanitize(String value) {
        if (value == null) {
            return "endpoint";
        }
        return NON_ALPHANUM.matcher(value).replaceAll("_");
    }

    public record DiagramArtifacts(
            Path classDiagram, Path erdPlantUml, Path erdMermaid, List<Path> sequenceDiagrams) {}
}
