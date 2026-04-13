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

class GraphMLExporterTest {

    private final GraphMLExporter exporter = new GraphMLExporter();

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
    void formatName_ReturnsGraphml() {
        assertThat(exporter.formatName()).isEqualTo("graphml");
    }

    @Test
    void fileExtension_ReturnsDotGraphml() {
        assertThat(exporter.fileExtension()).isEqualTo(".graphml");
    }

    @Nested
    class Given_GraphWithNodesAndEdges {

        @Nested
        class When_Exporting {

            @Test
            void Then_ValidXmlWithGraphmlNamespace() {
                var graph = buildTestGraph();
                var result = new String(exporter.export(graph), StandardCharsets.UTF_8);

                assertThat(result).contains("graphml");
                assertThat(result).contains("http://graphml.graphdrawing.org/xmlns");
                assertThat(result).contains("<node");
                assertThat(result).contains("<edge");
                assertThat(result).contains("UserService");
                assertThat(result).contains("findUser");
            }
        }
    }

    @Nested
    class Given_EmptyGraph {

        @Nested
        class When_Exporting {

            @Test
            void Then_ValidXmlWithEmptyGraph() {
                var graph = new KnowledgeGraph();
                var result = new String(exporter.export(graph), StandardCharsets.UTF_8);

                assertThat(result).contains("graphml");
                assertThat(result).contains("http://graphml.graphdrawing.org/xmlns");
                assertThat(result).doesNotContain("<node");
                assertThat(result).doesNotContain("<edge");
            }
        }
    }
}
