package com.codevision.codevisionbackend.graph.export;

import com.codevision.codevisionbackend.graph.KnowledgeGraph;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Orchestrator service that manages all {@link GraphExporter} implementations
 * and dispatches export requests by format name.
 */
@Service
public class GraphExportService {

    private final Map<String, GraphExporter> exportersByFormat;

    public GraphExportService(List<GraphExporter> exporters) {
        this.exportersByFormat = exporters.stream()
                .collect(Collectors.toMap(GraphExporter::formatName, Function.identity(),
                        (a, b) -> { throw new IllegalStateException(
                                "Duplicate exporter for format: " + a.formatName()); }));
    }

    /**
     * Exports the given graph in the specified format.
     *
     * @param graph  the graph to export
     * @param format the format name (e.g. "json", "csv")
     * @return raw bytes of the exported content
     * @throws IllegalArgumentException if the format is not supported
     */
    public byte[] export(KnowledgeGraph graph, String format) {
        var exporter = getExporter(format)
                .orElseThrow(() -> new IllegalArgumentException("Unsupported export format: " + format));
        return exporter.export(graph);
    }

    /**
     * Returns the list of all supported format names.
     *
     * @return sorted list of format names
     */
    public List<String> supportedFormats() {
        return exportersByFormat.keySet().stream()
                .sorted()
                .toList();
    }

    /**
     * Looks up an exporter by format name.
     *
     * @param format the format name
     * @return the exporter, or empty if not found
     */
    public Optional<GraphExporter> getExporter(String format) {
        if (format == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(exportersByFormat.get(format.toLowerCase()));
    }
}
