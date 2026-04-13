package com.codevision.codevisionbackend.graph.export;

import com.codevision.codevisionbackend.graph.*;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

class ObsidianExporterTest {

    private final ObsidianExporter exporter = new ObsidianExporter();

    private KnowledgeGraph buildTestGraph() {
        var graph = new KnowledgeGraph();
        graph.addNode(new KgNode("c1", KgNodeType.CLASS, "UserService", "com.app.UserService",
                new NodeMetadata("public", Set.of(), emptyList(), emptyList(), "void",
                        emptyList(), emptyList(), null, 0, 0, 0, null, null, 0, 0, null),
                null, "SOURCE", new Provenance("test", "Test.java", 1, ConfidenceLevel.EXTRACTED)));
        graph.addNode(new KgNode("m1", KgNodeType.METHOD, "findUser", "com.app.UserService.findUser",
                new NodeMetadata("public", Set.of(), emptyList(), emptyList(), "User",
                        emptyList(), emptyList(), null, 0, 0, 0, null, null, 0, 0, null),
                null, "SOURCE", new Provenance("test", "Test.java", 1, ConfidenceLevel.EXTRACTED)));
        graph.addEdge(new KgEdge("e1", KgEdgeType.DECLARES, "c1", "m1", "declares",
                ConfidenceLevel.EXTRACTED, new Provenance("test", "Test.java", 1, ConfidenceLevel.EXTRACTED), Map.of()));
        return graph;
    }

    @Test
    void formatName_ReturnsObsidian() {
        assertThat(exporter.formatName()).isEqualTo("obsidian");
    }

    @Test
    void fileExtension_ReturnsDotZip() {
        assertThat(exporter.fileExtension()).isEqualTo(".zip");
    }

    @Nested
    class Given_GraphWithNodesAndEdges {

        @Nested
        class When_Exporting {

            @Test
            void Then_ValidZipWithMarkdownFiles() throws IOException {
                var graph = buildTestGraph();
                var result = exporter.export(graph);

                assertThat(result).isNotEmpty();
                // Verify ZIP magic bytes: PK\x03\x04
                assertThat(result[0]).isEqualTo((byte) 0x50);
                assertThat(result[1]).isEqualTo((byte) 0x4B);

                // Extract and verify .md files exist
                var entryNames = new ArrayList<String>();
                try (var zis = new ZipInputStream(new ByteArrayInputStream(result))) {
                    ZipEntry entry;
                    while ((entry = zis.getNextEntry()) != null) {
                        entryNames.add(entry.getName());
                    }
                }
                assertThat(entryNames).isNotEmpty();
                assertThat(entryNames).anyMatch(name -> name.endsWith(".md"));
            }
        }
    }

    @Nested
    class Given_EmptyGraph {

        @Nested
        class When_Exporting {

            @Test
            void Then_ValidZipEvenIfEmpty() {
                var graph = new KnowledgeGraph();
                var result = exporter.export(graph);

                // Should still produce a valid (possibly minimal) byte array
                assertThat(result).isNotNull();
                assertThat(result.length).isGreaterThan(0);
            }
        }
    }
}
