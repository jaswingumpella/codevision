package com.codevision.codevisionbackend.analysis.multilang;

import com.codevision.codevisionbackend.graph.KgEdgeType;
import com.codevision.codevisionbackend.graph.KgNodeType;
import com.codevision.codevisionbackend.graph.KnowledgeGraph;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GoAnalyzerTest {

    private final GoAnalyzer analyzer = new GoAnalyzer();

    @Nested
    class Given_GoTreeWithFunctionDeclaration {

        @Nested
        class When_Analyzing {

            @Test
            void Then_CreatesFunctionNode() {
                var identNode = new ParsedNode("identifier", "main", 3, 3, 5, 9, List.of());
                var funcNode = new ParsedNode("function_declaration",
                        "func main() {}", 3, 3, 0, 15, List.of(identNode));
                var pkgIdent = new ParsedNode("package_identifier", "main", 1, 1, 8, 12, List.of());
                var pkgClause = new ParsedNode("package_clause", "package main",
                        1, 1, 0, 12, List.of(pkgIdent));
                var tree = new ParsedTree("main.go", "go", "package main\n\nfunc main() {}",
                        List.of(pkgClause, funcNode));
                var graph = new KnowledgeGraph();

                analyzer.analyzeTree(tree, graph);

                assertThat(graph.nodesOfType(KgNodeType.FUNCTION)).hasSize(1);
                var funcId = graph.nodesOfType(KgNodeType.FUNCTION).iterator().next();
                assertThat(graph.getNode(funcId).name()).isEqualTo("main");
            }

            @Test
            void Then_CreatesContainsEdge() {
                var identNode = new ParsedNode("identifier", "main", 3, 3, 5, 9, List.of());
                var funcNode = new ParsedNode("function_declaration",
                        "func main() {}", 3, 3, 0, 15, List.of(identNode));
                var pkgIdent = new ParsedNode("package_identifier", "main", 1, 1, 8, 12, List.of());
                var pkgClause = new ParsedNode("package_clause", "package main",
                        1, 1, 0, 12, List.of(pkgIdent));
                var tree = new ParsedTree("main.go", "go", "package main\n\nfunc main() {}",
                        List.of(pkgClause, funcNode));
                var graph = new KnowledgeGraph();

                analyzer.analyzeTree(tree, graph);

                var containsEdges = graph.edgesOfType(KgEdgeType.CONTAINS);
                assertThat(containsEdges).hasSizeGreaterThanOrEqualTo(2); // pkg->file, file->func
            }
        }
    }

    @Nested
    class Given_GoTreeWithStructType {

        @Nested
        class When_Analyzing {

            @Test
            void Then_CreatesStructNode() {
                var typeIdent = new ParsedNode("type_identifier", "User", 1, 1, 5, 9, List.of());
                var structType = new ParsedNode("struct_type", "struct {}", 1, 3, 10, 1, List.of());
                var typeSpec = new ParsedNode("type_spec", "User struct {}",
                        1, 3, 5, 1, List.of(typeIdent, structType));
                var typeDecl = new ParsedNode("type_declaration",
                        "type User struct {}", 1, 3, 0, 1, List.of(typeSpec));
                var tree = new ParsedTree("model.go", "go", "type User struct {}",
                        List.of(typeDecl));
                var graph = new KnowledgeGraph();

                analyzer.analyzeTree(tree, graph);

                assertThat(graph.nodesOfType(KgNodeType.STRUCT)).hasSize(1);
                var structId = graph.nodesOfType(KgNodeType.STRUCT).iterator().next();
                assertThat(graph.getNode(structId).name()).isEqualTo("User");
            }
        }
    }

    @Nested
    class Given_GoTreeWithStructFields {

        @Nested
        class When_Analyzing {

            @Test
            void Then_CreatesFieldNodes() {
                var nameFieldIdent = new ParsedNode("field_identifier", "Name", 2, 2, 4, 8, List.of());
                var nameFieldDecl = new ParsedNode("field_declaration", "Name string",
                        2, 2, 4, 15, List.of(nameFieldIdent));
                var ageFieldIdent = new ParsedNode("field_identifier", "Age", 3, 3, 4, 7, List.of());
                var ageFieldDecl = new ParsedNode("field_declaration", "Age int",
                        3, 3, 4, 11, List.of(ageFieldIdent));
                var fieldDeclList = new ParsedNode("field_declaration_list",
                        "{\n    Name string\n    Age int\n}", 1, 4, 17, 1,
                        List.of(nameFieldDecl, ageFieldDecl));
                var structType = new ParsedNode("struct_type", "struct { Name string; Age int }",
                        1, 4, 10, 1, List.of(fieldDeclList));
                var typeIdent = new ParsedNode("type_identifier", "Person", 1, 1, 5, 11, List.of());
                var typeSpec = new ParsedNode("type_spec", "Person struct { Name string; Age int }",
                        1, 4, 5, 1, List.of(typeIdent, structType));
                var typeDecl = new ParsedNode("type_declaration",
                        "type Person struct { Name string; Age int }", 1, 4, 0, 1,
                        List.of(typeSpec));
                var tree = new ParsedTree("model.go", "go",
                        "type Person struct { Name string; Age int }", List.of(typeDecl));
                var graph = new KnowledgeGraph();

                analyzer.analyzeTree(tree, graph);

                assertThat(graph.nodesOfType(KgNodeType.STRUCT)).hasSize(1);
                assertThat(graph.nodesOfType(KgNodeType.FIELD)).hasSize(2);
                var fieldIds = graph.nodesOfType(KgNodeType.FIELD);
                var fieldNames = fieldIds.stream()
                        .map(id -> graph.getNode(id).name())
                        .toList();
                assertThat(fieldNames).containsExactlyInAnyOrder("Name", "Age");
            }

            @Test
            void Then_CreatesContainsEdgesFromStructToFields() {
                var nameFieldIdent = new ParsedNode("field_identifier", "Name", 2, 2, 4, 8, List.of());
                var nameFieldDecl = new ParsedNode("field_declaration", "Name string",
                        2, 2, 4, 15, List.of(nameFieldIdent));
                var fieldDeclList = new ParsedNode("field_declaration_list",
                        "{\n    Name string\n}", 1, 3, 17, 1,
                        List.of(nameFieldDecl));
                var structType = new ParsedNode("struct_type", "struct { Name string }",
                        1, 3, 10, 1, List.of(fieldDeclList));
                var typeIdent = new ParsedNode("type_identifier", "Person", 1, 1, 5, 11, List.of());
                var typeSpec = new ParsedNode("type_spec", "Person struct { Name string }",
                        1, 3, 5, 1, List.of(typeIdent, structType));
                var typeDecl = new ParsedNode("type_declaration",
                        "type Person struct { Name string }", 1, 3, 0, 1,
                        List.of(typeSpec));
                var tree = new ParsedTree("model.go", "go",
                        "type Person struct { Name string }", List.of(typeDecl));
                var graph = new KnowledgeGraph();

                analyzer.analyzeTree(tree, graph);

                var containsEdges = graph.edgesOfType(KgEdgeType.CONTAINS);
                assertThat(containsEdges).anyMatch(e ->
                        e.sourceNodeId().contains("Person") && e.targetNodeId().contains("Name")
                                && e.label().equals("field"));
            }
        }
    }

    @Nested
    class Given_GoTreeWithInterfaceType {

        @Nested
        class When_Analyzing {

            @Test
            void Then_CreatesInterfaceNode() {
                var typeIdent = new ParsedNode("type_identifier", "Reader", 1, 1, 5, 11, List.of());
                var interfaceType = new ParsedNode("interface_type", "interface {}",
                        1, 3, 12, 1, List.of());
                var typeSpec = new ParsedNode("type_spec", "Reader interface {}",
                        1, 3, 5, 1, List.of(typeIdent, interfaceType));
                var typeDecl = new ParsedNode("type_declaration",
                        "type Reader interface {}", 1, 3, 0, 1, List.of(typeSpec));
                var tree = new ParsedTree("io.go", "go", "type Reader interface {}",
                        List.of(typeDecl));
                var graph = new KnowledgeGraph();

                analyzer.analyzeTree(tree, graph);

                assertThat(graph.nodesOfType(KgNodeType.INTERFACE)).hasSize(1);
                var ifaceId = graph.nodesOfType(KgNodeType.INTERFACE).iterator().next();
                assertThat(graph.getNode(ifaceId).name()).isEqualTo("Reader");
            }
        }
    }

    @Nested
    class Given_GoTreeWithMethodDeclaration {

        @Nested
        class When_Analyzing {

            @Test
            void Then_CreatesMethodNodeAndContainsEdge() {
                var receiverTypeIdent = new ParsedNode("type_identifier", "User",
                        1, 1, 8, 12, List.of());
                var receiverIdent = new ParsedNode("identifier", "u", 1, 1, 6, 7, List.of());
                var paramDecl = new ParsedNode("parameter_declaration", "u User",
                        1, 1, 6, 12, List.of(receiverIdent, receiverTypeIdent));
                var paramList = new ParsedNode("parameter_list", "(u User)",
                        1, 1, 5, 13, List.of(paramDecl));
                var fieldIdent = new ParsedNode("field_identifier", "Name",
                        1, 1, 15, 19, List.of());
                var methodNode = new ParsedNode("method_declaration",
                        "func (u User) Name() string {}", 1, 1, 0, 31,
                        List.of(paramList, fieldIdent));
                var tree = new ParsedTree("user.go", "go",
                        "func (u User) Name() string {}", List.of(methodNode));
                var graph = new KnowledgeGraph();

                analyzer.analyzeTree(tree, graph);

                assertThat(graph.nodesOfType(KgNodeType.METHOD)).hasSize(1);
                var methodId = graph.nodesOfType(KgNodeType.METHOD).iterator().next();
                assertThat(graph.getNode(methodId).name()).isEqualTo("Name");

                // Should have a CONTAINS edge from the struct receiver to the method
                var containsEdges = graph.edgesOfType(KgEdgeType.CONTAINS);
                assertThat(containsEdges).anyMatch(e ->
                        e.sourceNodeId().contains("User") && e.targetNodeId().contains("Name"));
            }
        }
    }

    @Nested
    class Given_GoTreeWithImportDeclaration {

        @Nested
        class When_Analyzing {

            @Test
            void Then_CreatesImportsEdge() {
                var importNode = new ParsedNode("import_declaration",
                        "import \"fmt\"", 2, 2, 0, 12, List.of());
                var tree = new ParsedTree("main.go", "go", "import \"fmt\"",
                        List.of(importNode));
                var graph = new KnowledgeGraph();

                analyzer.analyzeTree(tree, graph);

                var importsEdges = graph.edgesOfType(KgEdgeType.IMPORTS);
                assertThat(importsEdges).hasSize(1);
                assertThat(importsEdges.get(0).label()).contains("fmt");
            }
        }
    }
}
