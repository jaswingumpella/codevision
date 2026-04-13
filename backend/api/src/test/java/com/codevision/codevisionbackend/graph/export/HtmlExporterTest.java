package com.codevision.codevisionbackend.graph.export;

import com.codevision.codevisionbackend.graph.*;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

class HtmlExporterTest {

    private final HtmlExporter exporter = new HtmlExporter();

    private static KgNode node(String id, KgNodeType type, String name) {
        return new KgNode(id, type, name, "com.app." + name,
                new NodeMetadata("public", Set.of(), emptyList(), emptyList(), "void",
                        emptyList(), emptyList(), null, 0, 0, 0, null, null, 0, 0, null),
                null, "SOURCE", new Provenance("test", "Test.java", 1, ConfidenceLevel.EXTRACTED));
    }

    @Nested
    class Given_FormatMetadata {

        @Nested
        class When_Queried {

            @Test
            void Then_FormatNameIsHtml() {
                assertThat(exporter.formatName()).isEqualTo("html");
            }

            @Test
            void Then_FileExtensionIsDotHtml() {
                assertThat(exporter.fileExtension()).isEqualTo(".html");
            }

            @Test
            void Then_ContentTypeIsTextHtml() {
                assertThat(exporter.contentType()).isEqualTo("text/html");
            }
        }
    }

    @Nested
    class Given_GraphWithNodesAndEdges {

        @Nested
        class When_Exporting {

            @Test
            void Then_ContainsHtmlTableAndNodeData() {
                var graph = new KnowledgeGraph();
                graph.addNode(node("c1", KgNodeType.CLASS, "UserService"));
                graph.addNode(node("m1", KgNodeType.METHOD, "findUser"));
                graph.addEdge(new KgEdge("e1", KgEdgeType.DECLARES, "c1", "m1", "declares",
                        ConfidenceLevel.EXTRACTED, new Provenance("test", "Test.java", 1, ConfidenceLevel.EXTRACTED), Map.of()));

                var result = new String(exporter.export(graph), StandardCharsets.UTF_8);

                assertThat(result).contains("<html");
                assertThat(result).contains("<table");
                assertThat(result).contains("UserService");
                assertThat(result).contains("findUser");
                assertThat(result).contains("</html>");
            }

            @Test
            void Then_ContainsSearchScript() {
                var graph = new KnowledgeGraph();
                graph.addNode(node("c1", KgNodeType.CLASS, "UserService"));

                var result = new String(exporter.export(graph), StandardCharsets.UTF_8);

                assertThat(result).contains("filterTable");
                assertThat(result).contains("<script>");
            }

            @Test
            void Then_ContainsNodeAndEdgeCountStats() {
                var graph = new KnowledgeGraph();
                graph.addNode(node("c1", KgNodeType.CLASS, "UserService"));

                var result = new String(exporter.export(graph), StandardCharsets.UTF_8);

                assertThat(result).contains("<strong>Nodes:</strong> 1");
                assertThat(result).contains("<strong>Edges:</strong> 0");
            }
        }
    }

    @Nested
    class Given_GraphWithSpecialCharacters {

        @Nested
        class When_Exporting {

            @Test
            void Then_HtmlCharactersAreEscaped() {
                var graph = new KnowledgeGraph();
                graph.addNode(new KgNode("n1", KgNodeType.CLASS, "<Script>alert('xss')</Script>",
                        "com.app.<Script>",
                        new NodeMetadata("public", Set.of(), emptyList(), emptyList(), "void",
                                emptyList(), emptyList(), null, 0, 0, 0, null, null, 0, 0, null),
                        null, "SOURCE", new Provenance("test", "Test.java", 1, ConfidenceLevel.EXTRACTED)));

                var result = new String(exporter.export(graph), StandardCharsets.UTF_8);

                assertThat(result).contains("&lt;Script&gt;");
                assertThat(result).doesNotContain("<Script>alert");
            }

            @Test
            void Then_AmpersandsAreEscaped() {
                var graph = new KnowledgeGraph();
                graph.addNode(new KgNode("n1", KgNodeType.CLASS, "A&B",
                        "com.app.A&B",
                        new NodeMetadata("public", Set.of(), emptyList(), emptyList(), "void",
                                emptyList(), emptyList(), null, 0, 0, 0, null, null, 0, 0, null),
                        null, "SOURCE", new Provenance("test", "Test.java", 1, ConfidenceLevel.EXTRACTED)));

                var result = new String(exporter.export(graph), StandardCharsets.UTF_8);

                assertThat(result).contains("A&amp;B");
            }
        }
    }

    @Nested
    class Given_EmptyGraph {

        @Nested
        class When_Exporting {

            @Test
            void Then_ContainsHtmlStructureWithEmptyTable() {
                var graph = new KnowledgeGraph();
                var result = new String(exporter.export(graph), StandardCharsets.UTF_8);

                assertThat(result).contains("<html");
                assertThat(result).contains("</html>");
                assertThat(result).contains("<strong>Nodes:</strong> 0");
                assertThat(result).contains("<strong>Edges:</strong> 0");
            }
        }
    }
}
