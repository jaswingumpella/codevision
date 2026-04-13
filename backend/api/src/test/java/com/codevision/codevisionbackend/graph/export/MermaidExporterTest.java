package com.codevision.codevisionbackend.graph.export;

import com.codevision.codevisionbackend.graph.*;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

class MermaidExporterTest {

    private final MermaidExporter exporter = new MermaidExporter();

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
            void Then_FormatNameIsMermaid() {
                assertThat(exporter.formatName()).isEqualTo("mermaid");
            }

            @Test
            void Then_FileExtensionIsDotMmd() {
                assertThat(exporter.fileExtension()).isEqualTo(".mmd");
            }

            @Test
            void Then_ContentTypeIsTextPlain() {
                assertThat(exporter.contentType()).isEqualTo("text/plain");
            }
        }
    }

    @Nested
    class Given_GraphWithClassAndMethod {

        @Nested
        class When_Exporting {

            @Test
            void Then_ValidMermaidSyntax() {
                var graph = new KnowledgeGraph();
                graph.addNode(node("c1", KgNodeType.CLASS, "UserService"));
                graph.addNode(node("m1", KgNodeType.METHOD, "findUser"));
                graph.addEdge(edge("e1", KgEdgeType.DECLARES, "c1", "m1"));

                var result = new String(exporter.export(graph), StandardCharsets.UTF_8);

                assertThat(result).contains("flowchart");
                assertThat(result).contains("UserService");
                assertThat(result).contains("findUser");
                assertThat(result).contains("-->");
            }

            @Test
            void Then_ClassUsesRectangleBrackets() {
                var graph = new KnowledgeGraph();
                graph.addNode(node("c1", KgNodeType.CLASS, "MyClass"));

                var result = new String(exporter.export(graph), StandardCharsets.UTF_8);

                assertThat(result).contains("c1[MyClass]");
            }

            @Test
            void Then_MethodUsesRoundBrackets() {
                var graph = new KnowledgeGraph();
                graph.addNode(node("m1", KgNodeType.METHOD, "myMethod"));

                var result = new String(exporter.export(graph), StandardCharsets.UTF_8);

                assertThat(result).contains("m1(myMethod)");
            }
        }
    }

    @Nested
    class Given_GraphWithInterfaceNode {

        @Nested
        class When_Exporting {

            @Test
            void Then_InterfaceUsesCurlyBraces() {
                var graph = new KnowledgeGraph();
                graph.addNode(node("i1", KgNodeType.INTERFACE, "Repository"));

                var result = new String(exporter.export(graph), StandardCharsets.UTF_8);

                assertThat(result).contains("i1{Repository}");
            }
        }
    }

    @Nested
    class Given_GraphWithEnumNode {

        @Nested
        class When_Exporting {

            @Test
            void Then_EnumUsesRectangleBrackets() {
                var graph = new KnowledgeGraph();
                graph.addNode(node("e1", KgNodeType.ENUM, "Status"));

                var result = new String(exporter.export(graph), StandardCharsets.UTF_8);

                assertThat(result).contains("e1[Status]");
            }
        }
    }

    @Nested
    class Given_GraphWithPackageNode {

        @Nested
        class When_Exporting {

            @Test
            void Then_PackageUsesDoubleBrackets() {
                var graph = new KnowledgeGraph();
                graph.addNode(node("p1", KgNodeType.PACKAGE, "com.app"));

                var result = new String(exporter.export(graph), StandardCharsets.UTF_8);

                assertThat(result).contains("p1[[com.app]]");
            }
        }
    }

    @Nested
    class Given_GraphWithTraitNode {

        @Nested
        class When_Exporting {

            @Test
            void Then_TraitUsesCurlyBraces() {
                var graph = new KnowledgeGraph();
                graph.addNode(node("t1", KgNodeType.TRAIT, "Serializable"));

                var result = new String(exporter.export(graph), StandardCharsets.UTF_8);

                assertThat(result).contains("t1{Serializable}");
            }
        }
    }

    @Nested
    class Given_GraphWithRecordNode {

        @Nested
        class When_Exporting {

            @Test
            void Then_RecordUsesRectangleBrackets() {
                var graph = new KnowledgeGraph();
                graph.addNode(node("r1", KgNodeType.RECORD, "UserDTO"));

                var result = new String(exporter.export(graph), StandardCharsets.UTF_8);

                assertThat(result).contains("r1[UserDTO]");
            }
        }
    }

    @Nested
    class Given_GraphWithStructNode {

        @Nested
        class When_Exporting {

            @Test
            void Then_StructUsesRectangleBrackets() {
                var graph = new KnowledgeGraph();
                graph.addNode(node("s1", KgNodeType.STRUCT, "Point"));

                var result = new String(exporter.export(graph), StandardCharsets.UTF_8);

                assertThat(result).contains("s1[Point]");
            }
        }
    }

    @Nested
    class Given_GraphWithFileNode {

        @Nested
        class When_Exporting {

            @Test
            void Then_FileDefaultsToRectangleBrackets() {
                var graph = new KnowledgeGraph();
                graph.addNode(node("f1", KgNodeType.FILE, "App.java"));

                var result = new String(exporter.export(graph), StandardCharsets.UTF_8);

                // FILE falls to default branch -> rectangle brackets
                assertThat(result).contains("f1[App.java]");
            }
        }
    }

    @Nested
    class Given_GraphWithAnnotationTypeNode {

        @Nested
        class When_Exporting {

            @Test
            void Then_AnnotationDefaultsToRectangleBrackets() {
                var graph = new KnowledgeGraph();
                graph.addNode(node("a1", KgNodeType.ANNOTATION_TYPE, "Override"));

                var result = new String(exporter.export(graph), StandardCharsets.UTF_8);

                assertThat(result).contains("a1[Override]");
            }
        }
    }

    @Nested
    class Given_GraphWithDefaultTypeNode {

        @Nested
        class When_Exporting {

            @Test
            void Then_DefaultsToRectangleBrackets() {
                var graph = new KnowledgeGraph();
                graph.addNode(node("f1", KgNodeType.FIELD, "myField"));

                var result = new String(exporter.export(graph), StandardCharsets.UTF_8);

                assertThat(result).contains("f1[myField]");
            }
        }
    }

    @Nested
    class Given_GraphWithSpecialCharacters {

        @Nested
        class When_ExportingNodeWithDots {

            @Test
            void Then_IdIsSanitized() {
                var graph = new KnowledgeGraph();
                graph.addNode(node("com.app.Service", KgNodeType.CLASS, "Service"));

                var result = new String(exporter.export(graph), StandardCharsets.UTF_8);

                assertThat(result).contains("com_app_Service[Service]");
            }
        }
    }

    @Nested
    class Given_EmptyGraph {

        @Nested
        class When_Exporting {

            @Test
            void Then_JustHeader() {
                var graph = new KnowledgeGraph();
                var result = new String(exporter.export(graph), StandardCharsets.UTF_8);

                assertThat(result).contains("flowchart");
                assertThat(result).doesNotContain("-->");
            }
        }
    }
}
