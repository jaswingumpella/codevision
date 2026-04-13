package com.codevision.codevisionbackend.graph.export;

import com.codevision.codevisionbackend.graph.*;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

class CsvExporterTest {

    private final CsvExporter exporter = new CsvExporter();

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
    void formatName_ReturnsCsv() {
        assertThat(exporter.formatName()).isEqualTo("csv");
    }

    @Test
    void fileExtension_ReturnsDotCsv() {
        assertThat(exporter.fileExtension()).isEqualTo(".csv");
    }

    @Nested
    class Given_GraphWithNodesAndEdges {

        @Nested
        class When_Exporting {

            @Test
            void Then_ContainsHeaderRowsAndData() {
                var graph = buildTestGraph();
                var result = new String(exporter.export(graph), StandardCharsets.UTF_8);

                // Should contain node header and data
                assertThat(result).contains("id");
                assertThat(result).contains("type");
                assertThat(result).contains("name");
                assertThat(result).contains("UserService");
                assertThat(result).contains("findUser");
                // Should contain edge data
                assertThat(result).contains("DECLARES");
                assertThat(result).contains("c1");
                assertThat(result).contains("m1");
            }
        }
    }

    @Nested
    class Given_EmptyGraph {

        @Nested
        class When_Exporting {

            @Test
            void Then_HeadersOnly() {
                var graph = new KnowledgeGraph();
                var result = new String(exporter.export(graph), StandardCharsets.UTF_8);

                // Should still have headers
                assertThat(result).contains("id");
                assertThat(result).contains("type");
                // Should not contain data rows beyond the header
                var lines = result.strip().split("\n");
                // At least header lines are present, but no data lines with actual node/edge ids
                assertThat(result).doesNotContain("UserService");
            }
        }
    }
}
