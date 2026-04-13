package com.codevision.codevisionbackend.graph.export;

import com.codevision.codevisionbackend.graph.KnowledgeGraph;

/**
 * Strategy interface for exporting a {@link KnowledgeGraph} into a specific
 * output format. Each implementation is a Spring {@code @Component} that
 * participates in auto-discovery via {@code List<GraphExporter>} injection.
 */
public interface GraphExporter {

    /**
     * Returns the human-readable format name (e.g. "json", "csv", "graphml").
     *
     * @return format name, lower-case
     */
    String formatName();

    /**
     * Returns the conventional file extension including the dot (e.g. ".json").
     *
     * @return file extension
     */
    String fileExtension();

    /**
     * Exports the given knowledge graph into this format.
     *
     * @param graph the graph to export; must not be {@code null}
     * @return raw bytes of the exported content
     */
    byte[] export(KnowledgeGraph graph);

    /**
     * Returns the MIME content type for this format (e.g. "application/json").
     * Defaults to "application/octet-stream" if not overridden.
     *
     * @return MIME type string
     */
    default String contentType() {
        return "application/octet-stream";
    }
}
