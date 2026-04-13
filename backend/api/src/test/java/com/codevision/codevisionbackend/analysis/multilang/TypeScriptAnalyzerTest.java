package com.codevision.codevisionbackend.analysis.multilang;

import com.codevision.codevisionbackend.graph.KgEdgeType;
import com.codevision.codevisionbackend.graph.KgNodeType;
import com.codevision.codevisionbackend.graph.KnowledgeGraph;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TypeScriptAnalyzerTest {

    private final TypeScriptAnalyzer analyzer = new TypeScriptAnalyzer();

    @Nested
    class Given_TypeScriptTreeWithClassAndFunction {

        @Nested
        class When_Analyzing {

            @Test
            void Then_CreatesClassNode() {
                var tree = tsTreeWithClassFunctionExport();
                var graph = new KnowledgeGraph();

                analyzer.analyzeTree(tree, graph);

                assertThat(graph.nodesOfType(KgNodeType.CLASS)).hasSize(1);
                var classId = graph.nodesOfType(KgNodeType.CLASS).iterator().next();
                assertThat(graph.getNode(classId).name()).isEqualTo("UserService");
            }

            @Test
            void Then_CreatesFunctionNode() {
                var tree = tsTreeWithClassFunctionExport();
                var graph = new KnowledgeGraph();

                analyzer.analyzeTree(tree, graph);

                assertThat(graph.nodesOfType(KgNodeType.METHOD)).hasSize(1);
                var methodId = graph.nodesOfType(KgNodeType.METHOD).iterator().next();
                assertThat(graph.getNode(methodId).name()).isEqualTo("processData");
            }

            @Test
            void Then_CreatesFileNode() {
                var tree = tsTreeWithClassFunctionExport();
                var graph = new KnowledgeGraph();

                analyzer.analyzeTree(tree, graph);

                assertThat(graph.nodesOfType(KgNodeType.FILE)).hasSize(1);
            }
        }
    }

    @Nested
    class Given_TypeScriptTreeWithExportedClass {

        @Nested
        class When_Analyzing {

            @Test
            void Then_CreatesExportsEdge() {
                var classNode = new ParsedNode("class_declaration",
                        "class UserService", 2, 10, 0, 1, List.of(
                        new ParsedNode("identifier", "UserService", 2, 2, 6, 17, List.of()),
                        new ParsedNode("class_body", "{}", 2, 10, 18, 1, List.of())
                ));
                var exportNode = new ParsedNode("export_statement",
                        "export class UserService { }", 1, 10, 0, 1, List.of(classNode));
                var tree = new ParsedTree("service.ts", "typescript", "source",
                        List.of(exportNode));
                var graph = new KnowledgeGraph();

                analyzer.analyzeTree(tree, graph);

                var exportEdges = graph.edgesOfType(KgEdgeType.EXPORTS);
                assertThat(exportEdges).hasSize(1);
                assertThat(exportEdges.get(0).label()).contains("UserService");
            }
        }
    }

    @Nested
    class Given_TypeScriptTreeWithInterface {

        @Nested
        class When_Analyzing {

            @Test
            void Then_CreatesInterfaceNode() {
                var ifaceNode = new ParsedNode("interface_declaration",
                        "interface User", 1, 5, 0, 1, List.of(
                        new ParsedNode("identifier", "User", 1, 1, 10, 14, List.of()),
                        new ParsedNode("object_type", "{ name: string }", 1, 5, 15, 1, List.of())
                ));
                var tree = new ParsedTree("types.ts", "typescript", "source",
                        List.of(ifaceNode));
                var graph = new KnowledgeGraph();

                analyzer.analyzeTree(tree, graph);

                assertThat(graph.nodesOfType(KgNodeType.INTERFACE)).hasSize(1);
                var ifaceId = graph.nodesOfType(KgNodeType.INTERFACE).iterator().next();
                assertThat(graph.getNode(ifaceId).name()).isEqualTo("User");
            }
        }
    }

    @Nested
    class Given_TypeScriptTreeWithTypeAlias {

        @Nested
        class When_Analyzing {

            @Test
            void Then_CreatesTypeAliasNode() {
                var typeNode = new ParsedNode("type_alias_declaration",
                        "type ID = string", 1, 1, 0, 16, List.of(
                        new ParsedNode("identifier", "ID", 1, 1, 5, 7, List.of())
                ));
                var tree = new ParsedTree("types.ts", "typescript", "source",
                        List.of(typeNode));
                var graph = new KnowledgeGraph();

                analyzer.analyzeTree(tree, graph);

                assertThat(graph.nodesOfType(KgNodeType.TYPE_ALIAS)).hasSize(1);
                var typeId = graph.nodesOfType(KgNodeType.TYPE_ALIAS).iterator().next();
                var node = graph.getNode(typeId);
                assertThat(node.name()).isEqualTo("ID");
                assertThat(node.metadata().languageSpecific()).containsEntry("isTypeAlias", true);
            }
        }
    }

    @Nested
    class Given_TypeScriptTreeWithImport {

        @Nested
        class When_Analyzing {

            @Test
            void Then_CreatesImportsEdge() {
                var importNode = new ParsedNode("import_statement",
                        "import { Component } from '@angular/core'",
                        1, 1, 0, 41, List.of());
                var tree = new ParsedTree("app.ts", "typescript", "source",
                        List.of(importNode));
                var graph = new KnowledgeGraph();

                analyzer.analyzeTree(tree, graph);

                var importsEdges = graph.edgesOfType(KgEdgeType.IMPORTS);
                assertThat(importsEdges).hasSize(1);
            }
        }
    }

    @Nested
    class Given_TypeScriptTreeWithArrowFunction {

        @Nested
        class When_Analyzing {

            @Test
            void Then_CreatesLambdaNode() {
                var arrowNode = new ParsedNode("arrow_function",
                        "() => console.log('hello')", 1, 1, 0, 26, List.of());
                var tree = new ParsedTree("util.ts", "typescript", "source",
                        List.of(arrowNode));
                var graph = new KnowledgeGraph();

                analyzer.analyzeTree(tree, graph);

                assertThat(graph.nodesOfType(KgNodeType.LAMBDA)).hasSize(1);
            }
        }
    }

    @Nested
    class Given_TypeScriptTreeWithMethodInClass {

        @Nested
        class When_Analyzing {

            @Test
            void Then_MethodIsContainedByClass() {
                var methodNode = new ParsedNode("method_definition",
                        "getUsers()", 3, 6, 4, 5, List.of(
                        new ParsedNode("identifier", "getUsers", 3, 3, 4, 12, List.of())
                ));
                var classBody = new ParsedNode("class_body", "{ ... }", 2, 8, 0, 1,
                        List.of(methodNode));
                var classNode = new ParsedNode("class_declaration",
                        "class UserService", 1, 8, 0, 1, List.of(
                        new ParsedNode("identifier", "UserService", 1, 1, 6, 17, List.of()),
                        classBody
                ));
                var tree = new ParsedTree("service.ts", "typescript", "source",
                        List.of(classNode));
                var graph = new KnowledgeGraph();

                analyzer.analyzeTree(tree, graph);

                var classId = graph.nodesOfType(KgNodeType.CLASS).iterator().next();
                var classEdges = graph.getNeighbors(classId);
                assertThat(classEdges).anyMatch(e ->
                        e.type() == KgEdgeType.CONTAINS
                                && graph.getNode(e.targetNodeId()) != null
                                && graph.getNode(e.targetNodeId()).name().equals("getUsers")
                );
            }
        }
    }

    // ── Fixtures ────────────────────────────────────────────────────────

    private ParsedTree tsTreeWithClassFunctionExport() {
        var classNode = new ParsedNode("class_declaration",
                "class UserService", 1, 5, 0, 1, List.of(
                new ParsedNode("identifier", "UserService", 1, 1, 6, 17, List.of()),
                new ParsedNode("class_body", "{}", 1, 5, 18, 1, List.of())
        ));
        var funcNode = new ParsedNode("function_declaration",
                "function processData()", 7, 10, 0, 1, List.of(
                new ParsedNode("identifier", "processData", 7, 7, 9, 20, List.of())
        ));
        return new ParsedTree("service.ts", "typescript", "source",
                List.of(classNode, funcNode));
    }
}
