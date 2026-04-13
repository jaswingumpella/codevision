package com.codevision.codevisionbackend.graph.export;

import com.codevision.codevisionbackend.graph.*;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

class PlantUmlExporterTest {

    private final PlantUmlExporter exporter = new PlantUmlExporter();

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
            void Then_FormatNameIsPlantUml() {
                assertThat(exporter.formatName()).isEqualTo("plantuml");
            }

            @Test
            void Then_FileExtensionIsDotPuml() {
                assertThat(exporter.fileExtension()).isEqualTo(".puml");
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
            void Then_ContainsStartAndEndUml() {
                var graph = new KnowledgeGraph();
                graph.addNode(node("c1", KgNodeType.CLASS, "UserService"));
                graph.addNode(node("m1", KgNodeType.METHOD, "findUser"));
                graph.addEdge(edge("e1", KgEdgeType.DECLARES, "c1", "m1"));

                var result = new String(exporter.export(graph), StandardCharsets.UTF_8);

                assertThat(result).contains("@startuml");
                assertThat(result).contains("@enduml");
                assertThat(result).contains("UserService");
                assertThat(result).contains("findUser");
            }

            @Test
            void Then_ClassUsesClassKeyword() {
                var graph = new KnowledgeGraph();
                graph.addNode(node("c1", KgNodeType.CLASS, "UserService"));

                var result = new String(exporter.export(graph), StandardCharsets.UTF_8);

                assertThat(result).contains("class UserService");
            }
        }
    }

    @Nested
    class Given_GraphWithInterfaceNode {

        @Nested
        class When_Exporting {

            @Test
            void Then_InterfaceUsesInterfaceKeyword() {
                var graph = new KnowledgeGraph();
                graph.addNode(node("i1", KgNodeType.INTERFACE, "Repository"));

                var result = new String(exporter.export(graph), StandardCharsets.UTF_8);

                assertThat(result).contains("interface Repository");
            }
        }
    }

    @Nested
    class Given_GraphWithEnumNode {

        @Nested
        class When_Exporting {

            @Test
            void Then_EnumUsesEnumKeyword() {
                var graph = new KnowledgeGraph();
                graph.addNode(node("e1", KgNodeType.ENUM, "Status"));

                var result = new String(exporter.export(graph), StandardCharsets.UTF_8);

                assertThat(result).contains("enum Status");
            }
        }
    }

    @Nested
    class Given_GraphWithAnnotationTypeNode {

        @Nested
        class When_Exporting {

            @Test
            void Then_AnnotationTypeUsesAnnotationKeyword() {
                var graph = new KnowledgeGraph();
                graph.addNode(node("a1", KgNodeType.ANNOTATION_TYPE, "Override"));

                var result = new String(exporter.export(graph), StandardCharsets.UTF_8);

                assertThat(result).contains("annotation Override");
            }
        }
    }

    @Nested
    class Given_GraphWithTraitNode {

        @Nested
        class When_Exporting {

            @Test
            void Then_TraitUsesInterfaceKeyword() {
                var graph = new KnowledgeGraph();
                graph.addNode(node("t1", KgNodeType.TRAIT, "Serializable"));

                var result = new String(exporter.export(graph), StandardCharsets.UTF_8);

                assertThat(result).contains("interface Serializable");
            }
        }
    }

    @Nested
    class Given_GraphWithRecordNode {

        @Nested
        class When_Exporting {

            @Test
            void Then_RecordUsesClassKeyword() {
                var graph = new KnowledgeGraph();
                graph.addNode(node("r1", KgNodeType.RECORD, "UserDTO"));

                var result = new String(exporter.export(graph), StandardCharsets.UTF_8);

                assertThat(result).contains("class UserDTO");
            }
        }
    }

    @Nested
    class Given_GraphWithStructNode {

        @Nested
        class When_Exporting {

            @Test
            void Then_StructUsesClassKeyword() {
                var graph = new KnowledgeGraph();
                graph.addNode(node("s1", KgNodeType.STRUCT, "Point"));

                var result = new String(exporter.export(graph), StandardCharsets.UTF_8);

                assertThat(result).contains("class Point");
            }
        }
    }

    @Nested
    class Given_GraphWithPackageNode {

        @Nested
        class When_Exporting {

            @Test
            void Then_PackageUsesClassKeyword() {
                var graph = new KnowledgeGraph();
                graph.addNode(node("p1", KgNodeType.PACKAGE, "com.app"));

                var result = new String(exporter.export(graph), StandardCharsets.UTF_8);

                // PACKAGE falls to default branch -> class keyword
                assertThat(result).contains("class com.app");
            }
        }
    }

    @Nested
    class Given_GraphWithFileNode {

        @Nested
        class When_Exporting {

            @Test
            void Then_FileUsesClassKeyword() {
                var graph = new KnowledgeGraph();
                graph.addNode(node("f1", KgNodeType.FILE, "App.java"));

                var result = new String(exporter.export(graph), StandardCharsets.UTF_8);

                assertThat(result).contains("class App.java");
            }
        }
    }

    @Nested
    class Given_GraphWithExtendsEdge {

        @Nested
        class When_Exporting {

            @Test
            void Then_UsesInheritanceArrow() {
                var graph = new KnowledgeGraph();
                graph.addNode(node("c1", KgNodeType.CLASS, "Parent"));
                graph.addNode(node("c2", KgNodeType.CLASS, "Child"));
                graph.addEdge(edge("e1", KgEdgeType.EXTENDS, "c2", "c1"));

                var result = new String(exporter.export(graph), StandardCharsets.UTF_8);

                assertThat(result).contains("--|>");
            }
        }
    }

    @Nested
    class Given_GraphWithImplementsEdge {

        @Nested
        class When_Exporting {

            @Test
            void Then_UsesRealizationArrow() {
                var graph = new KnowledgeGraph();
                graph.addNode(node("i1", KgNodeType.INTERFACE, "Repository"));
                graph.addNode(node("c1", KgNodeType.CLASS, "UserRepo"));
                graph.addEdge(edge("e1", KgEdgeType.IMPLEMENTS, "c1", "i1"));

                var result = new String(exporter.export(graph), StandardCharsets.UTF_8);

                assertThat(result).contains("..|>");
            }
        }
    }

    @Nested
    class Given_GraphWithUsesEdge {

        @Nested
        class When_Exporting {

            @Test
            void Then_UsesDashedArrow() {
                var graph = new KnowledgeGraph();
                graph.addNode(node("c1", KgNodeType.CLASS, "Service"));
                graph.addNode(node("c2", KgNodeType.CLASS, "Helper"));
                graph.addEdge(edge("e1", KgEdgeType.USES, "c1", "c2"));

                var result = new String(exporter.export(graph), StandardCharsets.UTF_8);

                assertThat(result).contains("..>");
            }
        }
    }

    @Nested
    class Given_GraphWithContainsEdge {

        @Nested
        class When_Exporting {

            @Test
            void Then_UsesCompositionArrow() {
                var graph = new KnowledgeGraph();
                graph.addNode(node("c1", KgNodeType.CLASS, "UserService"));
                graph.addNode(node("m1", KgNodeType.METHOD, "findUser"));
                graph.addEdge(edge("e1", KgEdgeType.CONTAINS, "c1", "m1"));

                var result = new String(exporter.export(graph), StandardCharsets.UTF_8);

                assertThat(result).contains("*--");
            }
        }
    }

    @Nested
    class Given_EmptyGraph {

        @Nested
        class When_Exporting {

            @Test
            void Then_JustStartAndEndUml() {
                var graph = new KnowledgeGraph();
                var result = new String(exporter.export(graph), StandardCharsets.UTF_8);

                assertThat(result).contains("@startuml");
                assertThat(result).contains("@enduml");
            }
        }
    }
}
