package com.codevision.codevisionbackend.graph.export;

import com.codevision.codevisionbackend.graph.*;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

class ExcelExporterTest {

    private final ExcelExporter exporter = new ExcelExporter();

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
    void formatName_ReturnsExcel() {
        assertThat(exporter.formatName()).isEqualTo("excel");
    }

    @Test
    void fileExtension_ReturnsDotXlsx() {
        assertThat(exporter.fileExtension()).isEqualTo(".xlsx");
    }

    @Nested
    class Given_GraphWithNodesAndEdges {

        @Nested
        class When_Exporting {

            @Test
            void Then_NonEmptyValidXlsxByteArray() {
                var graph = buildTestGraph();
                var result = exporter.export(graph);

                assertThat(result).isNotEmpty();
                // XLSX is a ZIP file, so it starts with PK magic bytes
                assertThat(result[0]).isEqualTo((byte) 0x50);
                assertThat(result[1]).isEqualTo((byte) 0x4B);
            }
        }
    }

    @Nested
    class Given_EmptyGraph {

        @Nested
        class When_Exporting {

            @Test
            void Then_StillProducesValidXlsx() {
                var graph = new KnowledgeGraph();
                var result = exporter.export(graph);

                assertThat(result).isNotEmpty();
                // XLSX is a ZIP file, so it starts with PK magic bytes
                assertThat(result[0]).isEqualTo((byte) 0x50);
                assertThat(result[1]).isEqualTo((byte) 0x4B);
            }
        }
    }
}
