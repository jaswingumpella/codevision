package com.codevision.codevisionbackend.analysis.multilang;

import com.codevision.codevisionbackend.graph.KgEdgeType;
import com.codevision.codevisionbackend.graph.KgNodeType;
import com.codevision.codevisionbackend.graph.KnowledgeGraph;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GenericTreeSitterAnalyzerTest {

    private final GenericTreeSitterAnalyzer analyzer = new GenericTreeSitterAnalyzer();

    @Nested
    class Given_ParsedTreeWithClassAndFunction {

        @Nested
        class When_Analyzing {

            @Test
            void Then_CreatesFileNode() {
                var tree = treeWithClassAndFunction();
                var graph = new KnowledgeGraph();

                analyzer.analyzeGeneric(tree, graph);

                assertThat(graph.nodesOfType(KgNodeType.FILE)).hasSize(1);
            }

            @Test
            void Then_CreatesClassNode() {
                var tree = treeWithClassAndFunction();
                var graph = new KnowledgeGraph();

                analyzer.analyzeGeneric(tree, graph);

                assertThat(graph.nodesOfType(KgNodeType.CLASS)).hasSize(1);
                var classId = graph.nodesOfType(KgNodeType.CLASS).iterator().next();
                assertThat(graph.getNode(classId).name()).isEqualTo("MyClass");
            }

            @Test
            void Then_CreatesMethodNode() {
                var tree = treeWithClassAndFunction();
                var graph = new KnowledgeGraph();

                analyzer.analyzeGeneric(tree, graph);

                assertThat(graph.nodesOfType(KgNodeType.METHOD)).hasSize(1);
                var methodId = graph.nodesOfType(KgNodeType.METHOD).iterator().next();
                assertThat(graph.getNode(methodId).name()).isEqualTo("myFunction");
            }

            @Test
            void Then_CreatesContainsEdges() {
                var tree = treeWithClassAndFunction();
                var graph = new KnowledgeGraph();

                analyzer.analyzeGeneric(tree, graph);

                var containsEdges = graph.edgesOfType(KgEdgeType.CONTAINS);
                assertThat(containsEdges).hasSizeGreaterThanOrEqualTo(2);
            }
        }
    }

    @Nested
    class Given_ParsedTreeWithInterfaceDeclaration {

        @Nested
        class When_Analyzing {

            @Test
            void Then_CreatesInterfaceNode() {
                var ifaceNode = new ParsedNode("interface_declaration", "interface Serializable",
                        1, 5, 0, 1, List.of(
                        new ParsedNode("identifier", "Serializable", 1, 1, 10, 22, List.of())
                ));
                var tree = new ParsedTree("file.lang", "generic", "source",
                        List.of(ifaceNode));
                var graph = new KnowledgeGraph();

                analyzer.analyzeGeneric(tree, graph);

                assertThat(graph.nodesOfType(KgNodeType.INTERFACE)).hasSize(1);
                var ifaceId = graph.nodesOfType(KgNodeType.INTERFACE).iterator().next();
                assertThat(graph.getNode(ifaceId).name()).isEqualTo("Serializable");
            }
        }
    }

    @Nested
    class Given_ParsedTreeWithImportStatements {

        @Nested
        class When_Analyzing {

            @Test
            void Then_CreatesImportsEdges() {
                var importNode = new ParsedNode("import_statement", "import os",
                        1, 1, 0, 9, List.of());
                var tree = new ParsedTree("file.lang", "generic", "import os",
                        List.of(importNode));
                var graph = new KnowledgeGraph();

                analyzer.analyzeGeneric(tree, graph);

                var importsEdges = graph.edgesOfType(KgEdgeType.IMPORTS);
                assertThat(importsEdges).hasSize(1);
                assertThat(importsEdges.get(0).label()).isEqualTo("import os");
            }
        }
    }

    @Nested
    class Given_ParsedTreeWithNestedStructure {

        @Nested
        class When_Analyzing {

            @Test
            void Then_CreatesMethodInsideClass() {
                var methodNode = new ParsedNode("method_definition", "def process(self)",
                        3, 5, 4, 1, List.of(
                        new ParsedNode("identifier", "process", 3, 3, 8, 15, List.of())
                ));
                var classNode = new ParsedNode("class_definition", "class Handler",
                        1, 6, 0, 1, List.of(
                        new ParsedNode("identifier", "Handler", 1, 1, 6, 13, List.of()),
                        methodNode
                ));
                var tree = new ParsedTree("handler.py", "generic", "source",
                        List.of(classNode));
                var graph = new KnowledgeGraph();

                analyzer.analyzeGeneric(tree, graph);

                assertThat(graph.nodesOfType(KgNodeType.CLASS)).hasSize(1);
                assertThat(graph.nodesOfType(KgNodeType.METHOD)).hasSize(1);

                // Verify the method is contained by the class
                var classId = graph.nodesOfType(KgNodeType.CLASS).iterator().next();
                var classEdges = graph.getNeighbors(classId);
                assertThat(classEdges).anyMatch(e ->
                        e.type() == KgEdgeType.CONTAINS
                                && graph.getNode(e.targetNodeId()) != null
                                && graph.getNode(e.targetNodeId()).type() == KgNodeType.METHOD
                );
            }
        }
    }

    @Nested
    class Given_EmptyParsedTree {

        @Nested
        class When_Analyzing {

            @Test
            void Then_CreatesOnlyFileNode() {
                var tree = new ParsedTree("empty.txt", "generic", "", List.of());
                var graph = new KnowledgeGraph();

                analyzer.analyzeGeneric(tree, graph);

                assertThat(graph.nodeCount()).isEqualTo(1);
                assertThat(graph.nodesOfType(KgNodeType.FILE)).hasSize(1);
            }
        }
    }

    // ── Fixtures ────────────────────────────────────────────────────────

    private ParsedTree treeWithClassAndFunction() {
        var classBody = new ParsedNode("class_definition", "class MyClass",
                1, 5, 0, 1, List.of(
                new ParsedNode("identifier", "MyClass", 1, 1, 6, 13, List.of())
        ));
        var functionNode = new ParsedNode("function_definition", "function myFunction()",
                7, 10, 0, 1, List.of(
                new ParsedNode("identifier", "myFunction", 7, 7, 9, 19, List.of())
        ));
        return new ParsedTree("test.lang", "generic", "source",
                List.of(classBody, functionNode));
    }
}
