package com.codevision.codevisionbackend.analysis.multilang;

import com.codevision.codevisionbackend.graph.KgEdgeType;
import com.codevision.codevisionbackend.graph.KgNodeType;
import com.codevision.codevisionbackend.graph.KnowledgeGraph;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PythonAnalyzerTest {

    private final PythonAnalyzer analyzer = new PythonAnalyzer();

    @Nested
    class Given_PythonTreeWithClassDefinition {

        @Nested
        class When_Analyzing {

            @Test
            void Then_CreatesClassNode() {
                var identNode = new ParsedNode("identifier", "MyClass", 1, 1, 6, 13, List.of());
                var body = new ParsedNode("block", "    pass", 2, 2, 4, 8, List.of());
                var classNode = new ParsedNode("class_definition",
                        "class MyClass:\n    pass", 1, 2, 0, 8, List.of(identNode, body));
                var tree = new ParsedTree("app.py", "python", "class MyClass:\n    pass",
                        List.of(classNode));
                var graph = new KnowledgeGraph();

                analyzer.analyzeTree(tree, graph);

                assertThat(graph.nodesOfType(KgNodeType.CLASS)).hasSize(1);
                var classId = graph.nodesOfType(KgNodeType.CLASS).iterator().next();
                assertThat(graph.getNode(classId).name()).isEqualTo("MyClass");
            }

            @Test
            void Then_CreatesContainsEdgeFromFileToClass() {
                var identNode = new ParsedNode("identifier", "MyClass", 1, 1, 6, 13, List.of());
                var body = new ParsedNode("block", "    pass", 2, 2, 4, 8, List.of());
                var classNode = new ParsedNode("class_definition",
                        "class MyClass:\n    pass", 1, 2, 0, 8, List.of(identNode, body));
                var tree = new ParsedTree("app.py", "python", "class MyClass:\n    pass",
                        List.of(classNode));
                var graph = new KnowledgeGraph();

                analyzer.analyzeTree(tree, graph);

                var containsEdges = graph.edgesOfType(KgEdgeType.CONTAINS);
                assertThat(containsEdges).isNotEmpty();
                assertThat(containsEdges).anyMatch(e ->
                        e.sourceNodeId().contains("app.py") && e.targetNodeId().contains("MyClass"));
            }
        }
    }

    @Nested
    class Given_PythonTreeWithClassExtendingSuperclass {

        @Nested
        class When_Analyzing {

            @Test
            void Then_CreatesExtendsEdge() {
                var identNode = new ParsedNode("identifier", "Dog", 1, 1, 6, 9, List.of());
                var superIdent = new ParsedNode("identifier", "Animal", 1, 1, 10, 16, List.of());
                var argList = new ParsedNode("argument_list", "(Animal)",
                        1, 1, 9, 17, List.of(superIdent));
                var body = new ParsedNode("block", "    pass", 2, 2, 4, 8, List.of());
                var classNode = new ParsedNode("class_definition",
                        "class Dog(Animal):\n    pass", 1, 2, 0, 8,
                        List.of(identNode, argList, body));
                var tree = new ParsedTree("animals.py", "python",
                        "class Dog(Animal):\n    pass", List.of(classNode));
                var graph = new KnowledgeGraph();

                analyzer.analyzeTree(tree, graph);

                var extendsEdges = graph.edgesOfType(KgEdgeType.EXTENDS);
                assertThat(extendsEdges).hasSize(1);
                assertThat(extendsEdges.get(0).label()).isEqualTo("Animal");
            }

            @Test
            void Then_CreatesMultipleExtendsEdgesForMultipleSuperclasses() {
                var identNode = new ParsedNode("identifier", "Dog", 1, 1, 6, 9, List.of());
                var superIdent1 = new ParsedNode("identifier", "Animal", 1, 1, 10, 16, List.of());
                var superIdent2 = new ParsedNode("identifier", "Pet", 1, 1, 18, 21, List.of());
                var argList = new ParsedNode("argument_list", "(Animal, Pet)",
                        1, 1, 9, 22, List.of(superIdent1, superIdent2));
                var body = new ParsedNode("block", "    pass", 2, 2, 4, 8, List.of());
                var classNode = new ParsedNode("class_definition",
                        "class Dog(Animal, Pet):\n    pass", 1, 2, 0, 8,
                        List.of(identNode, argList, body));
                var tree = new ParsedTree("animals.py", "python",
                        "class Dog(Animal, Pet):\n    pass", List.of(classNode));
                var graph = new KnowledgeGraph();

                analyzer.analyzeTree(tree, graph);

                var extendsEdges = graph.edgesOfType(KgEdgeType.EXTENDS);
                assertThat(extendsEdges).hasSize(2);
                assertThat(extendsEdges).anyMatch(e -> e.label().equals("Animal"));
                assertThat(extendsEdges).anyMatch(e -> e.label().equals("Pet"));
            }

            @Test
            void Then_AlsoCreatesClassNode() {
                var identNode = new ParsedNode("identifier", "Dog", 1, 1, 6, 9, List.of());
                var superIdent = new ParsedNode("identifier", "Animal", 1, 1, 10, 16, List.of());
                var argList = new ParsedNode("argument_list", "(Animal)",
                        1, 1, 9, 17, List.of(superIdent));
                var body = new ParsedNode("block", "    pass", 2, 2, 4, 8, List.of());
                var classNode = new ParsedNode("class_definition",
                        "class Dog(Animal):\n    pass", 1, 2, 0, 8,
                        List.of(identNode, argList, body));
                var tree = new ParsedTree("animals.py", "python",
                        "class Dog(Animal):\n    pass", List.of(classNode));
                var graph = new KnowledgeGraph();

                analyzer.analyzeTree(tree, graph);

                assertThat(graph.nodesOfType(KgNodeType.CLASS)).hasSize(1);
                var classId = graph.nodesOfType(KgNodeType.CLASS).iterator().next();
                assertThat(graph.getNode(classId).name()).isEqualTo("Dog");
            }
        }
    }

    @Nested
    class Given_PythonTreeWithFunctionDefinition {

        @Nested
        class When_Analyzing {

            @Test
            void Then_CreatesMethodNode() {
                var identNode = new ParsedNode("identifier", "process", 1, 1, 4, 11, List.of());
                var body = new ParsedNode("block", "    return None", 2, 2, 4, 15, List.of());
                var funcNode = new ParsedNode("function_definition",
                        "def process():\n    return None", 1, 2, 0, 15,
                        List.of(identNode, body));
                var tree = new ParsedTree("app.py", "python",
                        "def process():\n    return None", List.of(funcNode));
                var graph = new KnowledgeGraph();

                analyzer.analyzeTree(tree, graph);

                assertThat(graph.nodesOfType(KgNodeType.METHOD)).hasSize(1);
                var methodId = graph.nodesOfType(KgNodeType.METHOD).iterator().next();
                assertThat(graph.getNode(methodId).name()).isEqualTo("process");
            }
        }
    }

    @Nested
    class Given_PythonTreeWithImportStatement {

        @Nested
        class When_Analyzing {

            @Test
            void Then_CreatesImportsEdge() {
                var importNode = new ParsedNode("import_statement",
                        "import os", 1, 1, 0, 9, List.of());
                var tree = new ParsedTree("app.py", "python", "import os",
                        List.of(importNode));
                var graph = new KnowledgeGraph();

                analyzer.analyzeTree(tree, graph);

                var importsEdges = graph.edgesOfType(KgEdgeType.IMPORTS);
                assertThat(importsEdges).hasSize(1);
                assertThat(importsEdges.get(0).label()).contains("import os");
            }

            @Test
            void Then_CreatesImportsEdgeForFromImport() {
                var importNode = new ParsedNode("import_from_statement",
                        "from os import path", 1, 1, 0, 19, List.of());
                var tree = new ParsedTree("app.py", "python", "from os import path",
                        List.of(importNode));
                var graph = new KnowledgeGraph();

                analyzer.analyzeTree(tree, graph);

                var importsEdges = graph.edgesOfType(KgEdgeType.IMPORTS);
                assertThat(importsEdges).hasSize(1);
                assertThat(importsEdges.get(0).label()).contains("from os import path");
            }
        }
    }

    @Nested
    class Given_PythonTreeWithDecoratedDefinition {

        @Nested
        class When_Analyzing {

            @Test
            void Then_CreatesDecoratesEdge() {
                var decorator = new ParsedNode("decorator", "@staticmethod",
                        1, 1, 0, 13, List.of());
                var identNode = new ParsedNode("identifier", "helper", 2, 2, 4, 10, List.of());
                var body = new ParsedNode("block", "    pass", 3, 3, 4, 8, List.of());
                var funcNode = new ParsedNode("function_definition",
                        "def helper():\n    pass", 2, 3, 0, 8,
                        List.of(identNode, body));
                var decoratedNode = new ParsedNode("decorated_definition",
                        "@staticmethod\ndef helper():\n    pass", 1, 3, 0, 8,
                        List.of(decorator, funcNode));
                var tree = new ParsedTree("app.py", "python",
                        "@staticmethod\ndef helper():\n    pass",
                        List.of(decoratedNode));
                var graph = new KnowledgeGraph();

                analyzer.analyzeTree(tree, graph);

                var decoratesEdges = graph.edgesOfType(KgEdgeType.DECORATES);
                assertThat(decoratesEdges).hasSize(1);
                assertThat(decoratesEdges.get(0).label()).contains("@staticmethod");
            }

            @Test
            void Then_AlsoCreatesTheInnerFunctionNode() {
                var decorator = new ParsedNode("decorator", "@staticmethod",
                        1, 1, 0, 13, List.of());
                var identNode = new ParsedNode("identifier", "helper", 2, 2, 4, 10, List.of());
                var body = new ParsedNode("block", "    pass", 3, 3, 4, 8, List.of());
                var funcNode = new ParsedNode("function_definition",
                        "def helper():\n    pass", 2, 3, 0, 8,
                        List.of(identNode, body));
                var decoratedNode = new ParsedNode("decorated_definition",
                        "@staticmethod\ndef helper():\n    pass", 1, 3, 0, 8,
                        List.of(decorator, funcNode));
                var tree = new ParsedTree("app.py", "python",
                        "@staticmethod\ndef helper():\n    pass",
                        List.of(decoratedNode));
                var graph = new KnowledgeGraph();

                analyzer.analyzeTree(tree, graph);

                assertThat(graph.nodesOfType(KgNodeType.METHOD)).hasSize(1);
            }
        }
    }

    @Nested
    class Given_EmptyTree {

        @Nested
        class When_Analyzing {

            @Test
            void Then_CreatesOnlyFileNode() {
                var tree = new ParsedTree("empty.py", "python", "", List.of());
                var graph = new KnowledgeGraph();

                analyzer.analyzeTree(tree, graph);

                assertThat(graph.nodesOfType(KgNodeType.FILE)).hasSize(1);
                assertThat(graph.nodeCount()).isEqualTo(1);
                var fileId = graph.nodesOfType(KgNodeType.FILE).iterator().next();
                assertThat(graph.getNode(fileId).name()).isEqualTo("empty.py");
            }
        }
    }
}
