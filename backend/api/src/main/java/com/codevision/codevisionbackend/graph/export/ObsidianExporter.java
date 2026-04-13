package com.codevision.codevisionbackend.graph.export;

import com.codevision.codevisionbackend.graph.KnowledgeGraph;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Exports a {@link KnowledgeGraph} as an Obsidian-compatible markdown vault
 * packaged in a ZIP archive. Each node becomes a markdown file with
 * {@code [[wikilinks]]} to connected nodes.
 */
@Component
public class ObsidianExporter implements GraphExporter {

    @Override
    public String formatName() {
        return "obsidian";
    }

    @Override
    public String fileExtension() {
        return ".zip";
    }

    @Override
    public String contentType() {
        return "application/zip";
    }

    @Override
    public byte[] export(KnowledgeGraph graph) {
        try (var baos = new ByteArrayOutputStream();
             var zos = new ZipOutputStream(baos)) {

            for (var entry : graph.getNodes().entrySet()) {
                var node = entry.getValue();
                var fileName = sanitizeFileName(node.name() != null ? node.name() : node.id()) + ".md";
                var content = buildMarkdown(graph, node.id());

                var zipEntry = new ZipEntry(fileName);
                zos.putNextEntry(zipEntry);
                zos.write(content.getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }

            zos.finish();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new ExportException("Failed to export graph as Obsidian vault", e);
        }
    }

    private String buildMarkdown(KnowledgeGraph graph, String nodeId) {
        var node = graph.getNode(nodeId);
        var sb = new StringBuilder();

        sb.append("# ").append(node.name() != null ? node.name() : node.id()).append('\n');
        sb.append('\n');
        sb.append("**Type:** ").append(node.type() != null ? node.type().name() : "unknown").append('\n');
        if (node.qualifiedName() != null) {
            sb.append("**Qualified Name:** ").append(node.qualifiedName()).append('\n');
        }
        sb.append('\n');

        // Outgoing connections
        var outgoing = graph.getNeighbors(nodeId);
        if (!outgoing.isEmpty()) {
            sb.append("## Outgoing\n\n");
            for (var edge : outgoing) {
                var target = graph.getNode(edge.targetNodeId());
                var targetName = target != null && target.name() != null ? target.name() : edge.targetNodeId();
                sb.append("- ").append(edge.type() != null ? edge.type().name() : "RELATED")
                  .append(": [[").append(sanitizeFileName(targetName)).append("]]\n");
            }
            sb.append('\n');
        }

        // Incoming connections
        var incoming = graph.getIncoming(nodeId);
        if (!incoming.isEmpty()) {
            sb.append("## Incoming\n\n");
            for (var edge : incoming) {
                var source = graph.getNode(edge.sourceNodeId());
                var sourceName = source != null && source.name() != null ? source.name() : edge.sourceNodeId();
                sb.append("- ").append(edge.type() != null ? edge.type().name() : "RELATED")
                  .append(": [[").append(sanitizeFileName(sourceName)).append("]]\n");
            }
            sb.append('\n');
        }

        return sb.toString();
    }

    private String sanitizeFileName(String name) {
        if (name == null) {
            return "unknown";
        }
        return name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}
