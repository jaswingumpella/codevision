package com.codevision.codevisionbackend.graph.export;

import com.codevision.codevisionbackend.graph.*;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

class DotExporterTest {

    private final DotExporter exporter = new DotExporter();

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
            void Then_FormatNameIsDot() {
                assertThat(exporter.formatName()).isEqualTo("dot");
            }

            @Test
            void Then_FileExtensionIsDotDot() {
                assertThat(exporter.fileExtension()).isEqualTo(".dot");
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
            void Then_ValidDotSyntax() {
                var graph = new KnowledgeGraph();
                graph.addNode(node("c1", KgNodeType.CLASS, "UserService"));
                graph.addNode(node("m1", KgNodeType.METHOD, "findUser"));
                graph.addEdge(edge("e1", KgEdgeType.DECLARES, "c1", "m1"));

                var result = new String(exporter.export(graph), StandardCharsets.UTF_8);

                assertThat(result).contains("digraph");
                assertThat(result).contains("UserService");
                assertThat(result).contains("findUser");
                assertThat(result).contains("->");
                assertThat(result).contains("}");
            }

            @Test
            void Then_ClassUsesBoxShape() {
                var graph = new KnowledgeGraph();
                graph.addNode(node("c1", KgNodeType.CLASS, "UserService"));

                var result = new String(exporter.export(graph), StandardCharsets.UTF_8);

                assertThat(result).contains("shape=box");
            }

            @Test
            void Then_MethodUsesEllipseShape() {
                var graph = new KnowledgeGraph();
                graph.addNode(node("m1", KgNodeType.METHOD, "findUser"));

                var result = new String(exporter.export(graph), StandardCharsets.UTF_8);

                assertThat(result).contains("shape=ellipse");
            }
        }
    }

    @Nested
    class Given_GraphWithInterfaceNode {

        @Nested
        class When_Exporting {

            @Test
            void Then_InterfaceUsesDiamondShape() {
                var graph = new KnowledgeGraph();
                graph.addNode(node("i1", KgNodeType.INTERFACE, "Repository"));

                var result = new String(exporter.export(graph), StandardCharsets.UTF_8);

                assertThat(result).contains("shape=diamond");
            }
        }
    }

    @Nested
    class Given_GraphWithEnumNode {

        @Nested
        class When_Exporting {

            @Test
            void Then_EnumUsesBoxShape() {
                var graph = new KnowledgeGraph();
                graph.addNode(node("e1", KgNodeType.ENUM, "Status"));

                var result = new String(exporter.export(graph), StandardCharsets.UTF_8);

                assertThat(result).contains("shape=box");
            }
        }
    }

    @Nested
    class Given_GraphWithPackageNode {

        @Nested
        class When_Exporting {

            @Test
            void Then_PackageUsesFolderShape() {
                var graph = new KnowledgeGraph();
                graph.addNode(node("p1", KgNodeType.PACKAGE, "com.app"));

                var result = new String(exporter.export(graph), StandardCharsets.UTF_8);

                assertThat(result).contains("shape=folder");
            }
        }
    }

    @Nested
    class Given_GraphWithFileNode {

        @Nested
        class When_Exporting {

            @Test
            void Then_FileUsesNoteShape() {
                var graph = new KnowledgeGraph();
                graph.addNode(node("f1", KgNodeType.FILE, "App.java"));

                var result = new String(exporter.export(graph), StandardCharsets.UTF_8);

                assertThat(result).contains("shape=note");
            }
        }
    }

    @Nested
    class Given_GraphWithDefaultTypeFallback {

        @Nested
        class When_ExportingFieldNode {

            @Test
            void Then_DefaultsToEllipseShape() {
                var graph = new KnowledgeGraph();
                graph.addNode(node("f1", KgNodeType.FIELD, "myField"));

                var result = new String(exporter.export(graph), StandardCharsets.UTF_8);

                // FIELD is not in any explicit branch, so falls to default -> ellipse
                assertThat(result).contains("shape=ellipse");
            }
        }
    }

    @Nested
    class Given_GraphWithSpecialCharacters {

        @Nested
        class When_Exporting {

            @Test
            void Then_QuotesAreEscaped() {
                var graph = new KnowledgeGraph();
                graph.addNode(node("n1", KgNodeType.CLASS, "My\"Class"));

                var result = new String(exporter.export(graph), StandardCharsets.UTF_8);

                assertThat(result).contains("My\\\"Class");
                assertThat(result).doesNotContain("My\"Class\"");
            }
        }
    }

    @Nested
    class Given_EmptyGraph {

        @Nested
        class When_Exporting {

            @Test
            void Then_EmptyDigraph() {
                var graph = new KnowledgeGraph();
                var result = new String(exporter.export(graph), StandardCharsets.UTF_8);

                assertThat(result).contains("digraph");
                assertThat(result).contains("}");
                assertThat(result).doesNotContain("->");
            }
        }
    }

    @Nested
    class Given_GraphWithAllShapeTypes {

        @Nested
        class When_Exporting {

            @Test
            void Then_AllShapeBranchesCovered() {
                var graph = new KnowledgeGraph();
                graph.addNode(node("c1", KgNodeType.CLASS, "MyClass"));
                graph.addNode(node("r1", KgNodeType.RECORD, "MyRecord"));
                graph.addNode(node("s1", KgNodeType.STRUCT, "MyStruct"));
                graph.addNode(node("e1", KgNodeType.ENUM, "MyEnum"));
                graph.addNode(node("i1", KgNodeType.INTERFACE, "MyInterface"));
                graph.addNode(node("t1", KgNodeType.TRAIT, "MyTrait"));
                graph.addNode(node("pr1", KgNodeType.PROTOCOL, "MyProtocol"));
                graph.addNode(node("m1", KgNodeType.METHOD, "myMethod"));
                graph.addNode(node("ct1", KgNodeType.CONSTRUCTOR, "MyConstructor"));
                graph.addNode(node("f1", KgNodeType.FUNCTION, "myFunc"));
                graph.addNode(node("l1", KgNodeType.LAMBDA, "lambda"));
                graph.addNode(node("cl1", KgNodeType.CLOSURE, "closure"));
                graph.addNode(node("p1", KgNodeType.PACKAGE, "pkg"));
                graph.addNode(node("mo1", KgNodeType.MODULE, "mod"));
                graph.addNode(node("ns1", KgNodeType.NAMESPACE, "ns"));
                graph.addNode(node("fi1", KgNodeType.FILE, "file"));
                graph.addNode(node("fd1", KgNodeType.FIELD, "field"));

                var result = new String(exporter.export(graph), StandardCharsets.UTF_8);

                assertThat(result).contains("shape=box");
                assertThat(result).contains("shape=diamond");
                assertThat(result).contains("shape=ellipse");
                assertThat(result).contains("shape=folder");
                assertThat(result).contains("shape=note");
            }
        }
    }
}
