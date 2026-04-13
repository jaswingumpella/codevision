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

class PdfExporterTest {

    private final PdfExporter exporter = new PdfExporter(new PdfExportProperties());

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
    void formatName_ReturnsPdf() {
        assertThat(exporter.formatName()).isEqualTo("pdf");
    }

    @Test
    void fileExtension_ReturnsDotPdf() {
        assertThat(exporter.fileExtension()).isEqualTo(".pdf");
    }

    @Nested
    class Given_GraphWithNodesAndEdges {

        @Nested
        class When_Exporting {

            @Test
            void Then_NonEmptyByteArrayStartingWithPdfMagic() {
                var graph = buildTestGraph();
                var result = exporter.export(graph);

                assertThat(result).isNotEmpty();
                // PDF magic bytes: %PDF
                var header = new String(result, 0, Math.min(5, result.length), StandardCharsets.US_ASCII);
                assertThat(header).startsWith("%PDF");
            }
        }
    }

    @Nested
    class Given_EmptyGraph {

        @Nested
        class When_Exporting {

            @Test
            void Then_StillProducesValidPdf() {
                var graph = new KnowledgeGraph();
                var result = exporter.export(graph);

                assertThat(result).isNotEmpty();
                var header = new String(result, 0, Math.min(5, result.length), StandardCharsets.US_ASCII);
                assertThat(header).startsWith("%PDF");
            }
        }
    }
}
