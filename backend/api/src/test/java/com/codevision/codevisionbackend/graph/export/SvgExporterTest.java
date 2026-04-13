package com.codevision.codevisionbackend.graph.export;

import com.codevision.codevisionbackend.graph.*;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

class SvgExporterTest {

    private final SvgExporter exporter = new SvgExporter(new SvgExportProperties());

    private static KgNode node(String id, KgNodeType type, String name) {
        return new KgNode(id, type, name, "com.app." + name,
                new NodeMetadata("public", Set.of(), emptyList(), emptyList(), "void",
                        emptyList(), emptyList(), null, 0, 0, 0, null, null, 0, 0, null),
                null, "SOURCE", new Provenance("test", "Test.java", 1, ConfidenceLevel.EXTRACTED));
    }

    private static KgEdge edge(String id, KgEdgeType type, String src, String tgt) {
        return new KgEdge(id, type, src, tgt, type.name(),
                ConfidenceLevel.EXTRACTED, new Provenance("test", "Test.java", 1, ConfidenceLevel.EXTRACTED), Map.of());
    }

    @Nested
    class Given_FormatMetadata {

        @Nested
        class When_Queried {

            @Test
            void Then_FormatNameIsSvg() {
                assertThat(exporter.formatName()).isEqualTo("svg");
            }

            @Test
            void Then_FileExtensionIsDotSvg() {
                assertThat(exporter.fileExtension()).isEqualTo(".svg");
            }

            @Test
            void Then_ContentTypeIsImageSvgXml() {
                assertThat(exporter.contentType()).isEqualTo("image/svg+xml");
            }
        }
    }

    @Nested
    class Given_GraphWithClassNode {

        @Nested
        class When_Exporting {

            @Test
            void Then_RendersRectangle() {
                var graph = new KnowledgeGraph();
                graph.addNode(node("c1", KgNodeType.CLASS, "UserService"));

                var result = new String(exporter.export(graph), StandardCharsets.UTF_8);

                assertThat(result).contains("<svg");
                assertThat(result).contains("</svg>");
                assertThat(result).contains("<rect");
                assertThat(result).contains("node-rect");
                assertThat(result).contains("UserService");
            }
        }
    }

    @Nested
    class Given_GraphWithRecordNode {

        @Nested
        class When_Exporting {

            @Test
            void Then_RendersRectangle() {
                var graph = new KnowledgeGraph();
                graph.addNode(node("r1", KgNodeType.RECORD, "UserDTO"));

                var result = new String(exporter.export(graph), StandardCharsets.UTF_8);

                assertThat(result).contains("<rect");
                assertThat(result).contains("node-rect");
            }
        }
    }

    @Nested
    class Given_GraphWithEnumNode {

        @Nested
        class When_Exporting {

            @Test
            void Then_RendersRectangle() {
                var graph = new KnowledgeGraph();
                graph.addNode(node("e1", KgNodeType.ENUM, "Status"));

                var result = new String(exporter.export(graph), StandardCharsets.UTF_8);

                assertThat(result).contains("<rect");
                assertThat(result).contains("node-rect");
                assertThat(result).contains("Status");
            }
        }
    }

    @Nested
    class Given_GraphWithInterfaceNode {

        @Nested
        class When_Exporting {

            @Test
            void Then_RendersDiamond() {
                var graph = new KnowledgeGraph();
                graph.addNode(node("i1", KgNodeType.INTERFACE, "Repository"));

                var result = new String(exporter.export(graph), StandardCharsets.UTF_8);

                assertThat(result).contains("<polygon");
                assertThat(result).contains("node-diamond");
                assertThat(result).contains("Repository");
            }
        }
    }

    @Nested
    class Given_GraphWithTraitNode {

        @Nested
        class When_Exporting {

            @Test
            void Then_RendersDiamond() {
                var graph = new KnowledgeGraph();
                graph.addNode(node("t1", KgNodeType.TRAIT, "Cloneable"));

                var result = new String(exporter.export(graph), StandardCharsets.UTF_8);

                assertThat(result).contains("<polygon");
                assertThat(result).contains("node-diamond");
            }
        }
    }

    @Nested
    class Given_GraphWithMethodNode {

        @Nested
        class When_Exporting {

            @Test
            void Then_RendersCircle() {
                var graph = new KnowledgeGraph();
                graph.addNode(node("m1", KgNodeType.METHOD, "findUser"));

                var result = new String(exporter.export(graph), StandardCharsets.UTF_8);

                assertThat(result).contains("<circle");
                assertThat(result).contains("node-circle");
                assertThat(result).contains("findUser");
            }
        }
    }

    @Nested
    class Given_GraphWithPackageNode {

        @Nested
        class When_Exporting {

            @Test
            void Then_RendersCircleAsDefault() {
                var graph = new KnowledgeGraph();
                graph.addNode(node("p1", KgNodeType.PACKAGE, "com.app"));

                var result = new String(exporter.export(graph), StandardCharsets.UTF_8);

                // PACKAGE is not in rectangle or diamond type sets, so renders as circle
                assertThat(result).contains("<circle");
                assertThat(result).contains("node-circle");
            }
        }
    }

    @Nested
    class Given_GraphWithFileNode {

        @Nested
        class When_Exporting {

            @Test
            void Then_RendersCircleAsDefault() {
                var graph = new KnowledgeGraph();
                graph.addNode(node("f1", KgNodeType.FILE, "App.java"));

                var result = new String(exporter.export(graph), StandardCharsets.UTF_8);

                assertThat(result).contains("<circle");
            }
        }
    }

    @Nested
    class Given_GraphWithEdge {

        @Nested
        class When_Exporting {

            @Test
            void Then_RendersLine() {
                var graph = new KnowledgeGraph();
                graph.addNode(node("c1", KgNodeType.CLASS, "UserService"));
                graph.addNode(node("m1", KgNodeType.METHOD, "findUser"));
                graph.addEdge(edge("e1", KgEdgeType.DECLARES, "c1", "m1"));

                var result = new String(exporter.export(graph), StandardCharsets.UTF_8);

                assertThat(result).contains("<line");
                assertThat(result).contains("edge-line");
            }
        }
    }

    @Nested
    class Given_GraphWithStructNode {

        @Nested
        class When_Exporting {

            @Test
            void Then_RendersRectangle() {
                var graph = new KnowledgeGraph();
                graph.addNode(node("s1", KgNodeType.STRUCT, "Point"));

                var result = new String(exporter.export(graph), StandardCharsets.UTF_8);

                assertThat(result).contains("<rect");
                assertThat(result).contains("node-rect");
            }
        }
    }

    @Nested
    class Given_GraphWithAnnotationTypeNode {

        @Nested
        class When_Exporting {

            @Test
            void Then_RendersCircleAsDefault() {
                var graph = new KnowledgeGraph();
                graph.addNode(node("a1", KgNodeType.ANNOTATION_TYPE, "Override"));

                var result = new String(exporter.export(graph), StandardCharsets.UTF_8);

                // ANNOTATION_TYPE is not in rectangle or diamond sets, so falls to circle
                assertThat(result).contains("<circle");
            }
        }
    }

    @Nested
    class Given_GraphWithSpecialCharacters {

        @Nested
        class When_Exporting {

            @Test
            void Then_XmlCharactersAreEscaped() {
                var graph = new KnowledgeGraph();
                graph.addNode(node("n1", KgNodeType.CLASS, "List<String>"));

                var result = new String(exporter.export(graph), StandardCharsets.UTF_8);

                assertThat(result).contains("List&lt;String&gt;");
                assertThat(result).doesNotContain("List<String>");
            }
        }
    }

    @Nested
    class Given_EmptyGraph {

        @Nested
        class When_Exporting {

            @Test
            void Then_EmptySvg() {
                var graph = new KnowledgeGraph();
                var result = new String(exporter.export(graph), StandardCharsets.UTF_8);

                assertThat(result).contains("<svg");
                assertThat(result).contains("</svg>");
                assertThat(result).doesNotContain("<line");
            }
        }
    }

    @Nested
    class Given_GraphWithAllNodeShapes {

        @Nested
        class When_Exporting {

            @Test
            void Then_AllShapeTypesRendered() {
                var graph = new KnowledgeGraph();
                graph.addNode(node("c1", KgNodeType.CLASS, "MyClass"));
                graph.addNode(node("i1", KgNodeType.INTERFACE, "MyInterface"));
                graph.addNode(node("m1", KgNodeType.METHOD, "myMethod"));

                var result = new String(exporter.export(graph), StandardCharsets.UTF_8);

                assertThat(result).contains("<rect");
                assertThat(result).contains("<polygon");
                assertThat(result).contains("<circle");
            }
        }
    }
}
