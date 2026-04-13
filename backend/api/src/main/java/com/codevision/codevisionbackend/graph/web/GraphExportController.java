package com.codevision.codevisionbackend.graph.web;

import com.codevision.codevisionbackend.graph.KnowledgeGraph;
import com.codevision.codevisionbackend.graph.export.GraphExportService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller exposing graph export endpoints. Supports exporting a
 * {@link KnowledgeGraph} in any registered format and listing available formats.
 */
@RestController
@RequestMapping("/api/v1/graph/export")
public class GraphExportController {

    private final GraphExportService exportService;

    public GraphExportController(GraphExportService exportService) {
        this.exportService = exportService;
    }

    /**
     * Exports the given knowledge graph in the specified format.
     *
     * @param format the export format (e.g. "json", "csv", "graphml")
     * @param graph  the knowledge graph to export
     * @return the exported bytes with appropriate content type and disposition headers
     */
    @PostMapping("/{format}")
    public ResponseEntity<byte[]> exportGraph(
            @PathVariable String format,
            @RequestBody KnowledgeGraph graph) {

        var exporter = exportService.getExporter(format)
                .orElseThrow(() -> new IllegalArgumentException("Unsupported export format: " + format));

        var data = exporter.export(graph);
        var contentType = exporter.contentType();
        var fileName = "knowledge-graph" + exporter.fileExtension();
        var disposition = ContentDisposition.attachment().filename(fileName).build();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(data);
    }

    /**
     * Returns the list of supported export formats.
     *
     * @return map containing a {@code formats} key with the list of format names
     */
    @GetMapping("/formats")
    public ResponseEntity<Map<String, List<String>>> supportedFormats() {
        return ResponseEntity.ok(Map.of("formats", exportService.supportedFormats()));
    }

}
