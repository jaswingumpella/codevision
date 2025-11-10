package com.codevision.codevisionbackend.analysis;

import com.codevision.codevisionbackend.analysis.GraphModel.ClassNode;
import com.codevision.codevisionbackend.analysis.GraphModel.DependencyEdge;
import com.codevision.codevisionbackend.analysis.GraphModel.EndpointNode;
import com.codevision.codevisionbackend.analysis.GraphModel.SequenceNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Writes analysis JSON and CSV exports for downstream tooling.
 */
@Component
public class ExportWriter {

    private final ObjectMapper objectMapper;

    public ExportWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public AnalysisOutputPaths writeAll(GraphModel model, Path outputDir, DiagramWriter.DiagramArtifacts diagramArtifacts)
            throws IOException {
        Files.createDirectories(outputDir);
        Path analysisJson = outputDir.resolve("analysis.json");
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(analysisJson.toFile(), model);

        Path entitiesCsv = outputDir.resolve("entities.csv");
        Files.writeString(entitiesCsv, buildEntitiesCsv(model), StandardCharsets.UTF_8);

        Path sequencesCsv = outputDir.resolve("sequences.csv");
        Files.writeString(sequencesCsv, buildSequencesCsv(model), StandardCharsets.UTF_8);

        Path endpointsCsv = outputDir.resolve("endpoints.csv");
        Files.writeString(endpointsCsv, buildEndpointsCsv(model), StandardCharsets.UTF_8);

        Path dependenciesCsv = outputDir.resolve("dependencies.csv");
        Files.writeString(dependenciesCsv, buildDependenciesCsv(model), StandardCharsets.UTF_8);

        return new AnalysisOutputPaths(
                outputDir, analysisJson, entitiesCsv, sequencesCsv, endpointsCsv, dependenciesCsv, diagramArtifacts);
    }

    private String buildEntitiesCsv(GraphModel model) {
        String header = "className,packageName,tableName,origin,stereotypes\n";
        return header + model.sortedClasses().stream()
                .map(node -> csv(
                        node.getName(),
                        node.getPackageName(),
                        node.getTableName(),
                        node.getOrigin().name(),
                        String.join("|", node.getStereotypes())))
                .collect(Collectors.joining("\n"));
    }

    private String buildSequencesCsv(GraphModel model) {
        String header = "generatorName,sequenceName,allocationSize,initialValue\n";
        return header + model.getSequences().values().stream()
                .sorted(Comparator.comparing(SequenceNode::getGeneratorName, String.CASE_INSENSITIVE_ORDER))
                .map(node -> csv(
                        node.getGeneratorName(),
                        node.getSequenceName(),
                        valueOrEmpty(node.getAllocationSize()),
                        valueOrEmpty(node.getInitialValue())))
                .collect(Collectors.joining("\n"));
    }

    private String buildEndpointsCsv(GraphModel model) {
        String header = "type,httpMethod,path,controllerClass,controllerMethod,framework\n";
        return header + model.sortedEndpoints().stream()
                .map(endpoint -> csv(
                        endpoint.getType().name(),
                        endpoint.getHttpMethod(),
                        endpoint.getPath(),
                        endpoint.getControllerClass(),
                        endpoint.getControllerMethod(),
                        endpoint.getFramework()))
                .collect(Collectors.joining("\n"));
    }

    private String buildDependenciesCsv(GraphModel model) {
        String header = "fromClass,toClass,kind,label\n";
        return header + model.getDependencyEdges().stream()
                .sorted(Comparator
                        .comparing(DependencyEdge::getFromClass, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                        .thenComparing(DependencyEdge::getToClass, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .map(edge -> csv(
                        edge.getFromClass(),
                        edge.getToClass(),
                        edge.getKind() != null ? edge.getKind().name() : "",
                        edge.getLabel()))
                .collect(Collectors.joining("\n"));
    }

    private String csv(Object... values) {
        return List.of(values).stream()
                .map(value -> value == null ? "" : escape(value.toString()))
                .collect(Collectors.joining(","));
    }

    private String escape(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private String valueOrEmpty(Number number) {
        return number == null ? "" : number.toString();
    }
}
