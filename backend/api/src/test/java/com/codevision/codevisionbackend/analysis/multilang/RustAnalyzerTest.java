package com.codevision.codevisionbackend.analysis.multilang;

import com.codevision.codevisionbackend.graph.KgEdgeType;
import com.codevision.codevisionbackend.graph.KgNodeType;
import com.codevision.codevisionbackend.graph.KnowledgeGraph;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RustAnalyzerTest {

    private final RustAnalyzer analyzer = new RustAnalyzer();

    @Nested
    class Given_RustTreeWithStructItem {

        @Nested
        class When_Analyzing {

            @Test
            void Then_CreatesStructNode() {
                var identNode = new ParsedNode("type_identifier", "Point", 1, 1, 7, 12, List.of());
                var structNode = new ParsedNode("struct_item",
                        "struct Point { x: i32, y: i32 }", 1, 1, 0, 31,
                        List.of(identNode));
                var tree = new ParsedTree("lib.rs", "rust",
                        "struct Point { x: i32, y: i32 }", List.of(structNode));
                var graph = new KnowledgeGraph();

                analyzer.analyzeTree(tree, graph);

                assertThat(graph.nodesOfType(KgNodeType.STRUCT)).hasSize(1);
                var structId = graph.nodesOfType(KgNodeType.STRUCT).iterator().next();
                assertThat(graph.getNode(structId).name()).isEqualTo("Point");
            }

            @Test
            void Then_CreatesContainsEdgeFromFileToStruct() {
                var identNode = new ParsedNode("type_identifier", "Point", 1, 1, 7, 12, List.of());
                var structNode = new ParsedNode("struct_item",
                        "struct Point { x: i32, y: i32 }", 1, 1, 0, 31,
                        List.of(identNode));
                var tree = new ParsedTree("lib.rs", "rust",
                        "struct Point { x: i32, y: i32 }", List.of(structNode));
                var graph = new KnowledgeGraph();

                analyzer.analyzeTree(tree, graph);

                var containsEdges = graph.edgesOfType(KgEdgeType.CONTAINS);
                assertThat(containsEdges).anyMatch(e ->
                        e.sourceNodeId().contains("lib.rs") && e.targetNodeId().contains("Point"));
            }
        }
    }

    @Nested
    class Given_RustTreeWithFunctionItem {

        @Nested
        class When_Analyzing {

            @Test
            void Then_CreatesFunctionNode() {
                var identNode = new ParsedNode("identifier", "calculate", 1, 1, 3, 12, List.of());
                var funcNode = new ParsedNode("function_item",
                        "fn calculate() -> i32 { 42 }", 1, 1, 0, 28,
                        List.of(identNode));
                var tree = new ParsedTree("lib.rs", "rust",
                        "fn calculate() -> i32 { 42 }", List.of(funcNode));
                var graph = new KnowledgeGraph();

                analyzer.analyzeTree(tree, graph);

                assertThat(graph.nodesOfType(KgNodeType.FUNCTION)).hasSize(1);
                var funcId = graph.nodesOfType(KgNodeType.FUNCTION).iterator().next();
                assertThat(graph.getNode(funcId).name()).isEqualTo("calculate");
            }
        }
    }

    @Nested
    class Given_RustTreeWithTraitItem {

        @Nested
        class When_Analyzing {

            @Test
            void Then_CreatesTraitNode() {
                var identNode = new ParsedNode("type_identifier", "Drawable", 1, 1, 6, 14, List.of());
                var declList = new ParsedNode("declaration_list", "{ fn draw(&self); }",
                        1, 3, 15, 1, List.of());
                var traitNode = new ParsedNode("trait_item",
                        "trait Drawable { fn draw(&self); }", 1, 3, 0, 1,
                        List.of(identNode, declList));
                var tree = new ParsedTree("lib.rs", "rust",
                        "trait Drawable { fn draw(&self); }", List.of(traitNode));
                var graph = new KnowledgeGraph();

                analyzer.analyzeTree(tree, graph);

                assertThat(graph.nodesOfType(KgNodeType.TRAIT)).hasSize(1);
                var traitId = graph.nodesOfType(KgNodeType.TRAIT).iterator().next();
                assertThat(graph.getNode(traitId).name()).isEqualTo("Drawable");
            }
        }
    }

    @Nested
    class Given_RustTreeWithImplItem {

        @Nested
        class When_Analyzing {

            @Test
            void Then_CreatesImplementsEdge() {
                // impl Drawable for Circle
                var traitIdent = new ParsedNode("type_identifier", "Drawable",
                        1, 1, 5, 13, List.of());
                var forKeyword = new ParsedNode("for", "for", 1, 1, 14, 17, List.of());
                var typeIdent = new ParsedNode("type_identifier", "Circle",
                        1, 1, 18, 24, List.of());
                var declList = new ParsedNode("declaration_list", "{}",
                        1, 3, 25, 1, List.of());
                var implNode = new ParsedNode("impl_item",
                        "impl Drawable for Circle {}", 1, 3, 0, 1,
                        List.of(traitIdent, forKeyword, typeIdent, declList));
                var tree = new ParsedTree("lib.rs", "rust",
                        "impl Drawable for Circle {}", List.of(implNode));
                var graph = new KnowledgeGraph();

                analyzer.analyzeTree(tree, graph);

                var implementsEdges = graph.edgesOfType(KgEdgeType.IMPLEMENTS);
                assertThat(implementsEdges).hasSize(1);
                assertThat(implementsEdges.get(0).label()).contains("Circle");
                assertThat(implementsEdges.get(0).label()).contains("Drawable");
            }
        }
    }

    @Nested
    class Given_RustTreeWithModItem {

        @Nested
        class When_Analyzing {

            @Test
            void Then_CreatesNamespaceNode() {
                var identNode = new ParsedNode("identifier", "network", 1, 1, 4, 11, List.of());
                var modNode = new ParsedNode("mod_item",
                        "mod network {}", 1, 1, 0, 14,
                        List.of(identNode));
                var tree = new ParsedTree("lib.rs", "rust",
                        "mod network {}", List.of(modNode));
                var graph = new KnowledgeGraph();

                analyzer.analyzeTree(tree, graph);

                assertThat(graph.nodesOfType(KgNodeType.NAMESPACE)).hasSize(1);
                var nsId = graph.nodesOfType(KgNodeType.NAMESPACE).iterator().next();
                assertThat(graph.getNode(nsId).name()).isEqualTo("network");
            }

            @Test
            void Then_CreatesContainsEdgeFromFileToModule() {
                var identNode = new ParsedNode("identifier", "network", 1, 1, 4, 11, List.of());
                var modNode = new ParsedNode("mod_item",
                        "mod network {}", 1, 1, 0, 14,
                        List.of(identNode));
                var tree = new ParsedTree("lib.rs", "rust",
                        "mod network {}", List.of(modNode));
                var graph = new KnowledgeGraph();

                analyzer.analyzeTree(tree, graph);

                var containsEdges = graph.edgesOfType(KgEdgeType.CONTAINS);
                assertThat(containsEdges).anyMatch(e ->
                        e.sourceNodeId().contains("lib.rs") && e.targetNodeId().contains("network"));
            }
        }
    }

    @Nested
    class Given_RustTreeWithEnumItem {

        @Nested
        class When_Analyzing {

            @Test
            void Then_CreatesEnumNode() {
                var identNode = new ParsedNode("type_identifier", "Color", 1, 1, 5, 10, List.of());
                var enumNode = new ParsedNode("enum_item",
                        "enum Color { Red, Green, Blue }", 1, 1, 0, 31,
                        List.of(identNode));
                var tree = new ParsedTree("lib.rs", "rust",
                        "enum Color { Red, Green, Blue }", List.of(enumNode));
                var graph = new KnowledgeGraph();

                analyzer.analyzeTree(tree, graph);

                assertThat(graph.nodesOfType(KgNodeType.ENUM)).hasSize(1);
                var enumId = graph.nodesOfType(KgNodeType.ENUM).iterator().next();
                assertThat(graph.getNode(enumId).name()).isEqualTo("Color");
            }

            @Test
            void Then_CreatesContainsEdgeFromFileToEnum() {
                var identNode = new ParsedNode("type_identifier", "Color", 1, 1, 5, 10, List.of());
                var enumNode = new ParsedNode("enum_item",
                        "enum Color { Red, Green, Blue }", 1, 1, 0, 31,
                        List.of(identNode));
                var tree = new ParsedTree("lib.rs", "rust",
                        "enum Color { Red, Green, Blue }", List.of(enumNode));
                var graph = new KnowledgeGraph();

                analyzer.analyzeTree(tree, graph);

                var containsEdges = graph.edgesOfType(KgEdgeType.CONTAINS);
                assertThat(containsEdges).anyMatch(e ->
                        e.sourceNodeId().contains("lib.rs") && e.targetNodeId().contains("Color"));
            }
        }
    }

    @Nested
    class Given_RustTreeWithUseDeclaration {

        @Nested
        class When_Analyzing {

            @Test
            void Then_CreatesImportsEdge() {
                var useNode = new ParsedNode("use_declaration",
                        "use std::collections::HashMap;", 1, 1, 0, 30, List.of());
                var tree = new ParsedTree("lib.rs", "rust",
                        "use std::collections::HashMap;", List.of(useNode));
                var graph = new KnowledgeGraph();

                analyzer.analyzeTree(tree, graph);

                var importsEdges = graph.edgesOfType(KgEdgeType.IMPORTS);
                assertThat(importsEdges).hasSize(1);
                assertThat(importsEdges.get(0).label()).contains("std::collections::HashMap");
            }
        }
    }
}
