package com.codevision.codevisionbackend.analysis.multilang;

import com.codevision.codevisionbackend.graph.KgEdgeType;
import com.codevision.codevisionbackend.graph.KgNodeType;
import com.codevision.codevisionbackend.graph.KnowledgeGraph;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CSharpAnalyzerTest {

    private final CSharpAnalyzer analyzer = new CSharpAnalyzer();

    @Nested
    class Given_CSharpTreeWithNamespaceDeclaration {

        @Nested
        class When_Analyzing {

            @Test
            void Then_CreatesPackageNode() {
                var identNode = new ParsedNode("identifier", "MyApp.Models",
                        1, 1, 10, 22, List.of());
                var body = new ParsedNode("declaration_list", "{}",
                        2, 5, 0, 1, List.of());
                var nsNode = new ParsedNode("namespace_declaration",
                        "namespace MyApp.Models {}", 1, 5, 0, 1,
                        List.of(identNode, body));
                var tree = new ParsedTree("Program.cs", "csharp",
                        "namespace MyApp.Models {}", List.of(nsNode));
                var graph = new KnowledgeGraph();

                analyzer.analyzeTree(tree, graph);

                assertThat(graph.nodesOfType(KgNodeType.PACKAGE)).hasSize(1);
                var pkgId = graph.nodesOfType(KgNodeType.PACKAGE).iterator().next();
                assertThat(graph.getNode(pkgId).name()).isEqualTo("MyApp.Models");
            }
        }
    }

    @Nested
    class Given_CSharpTreeWithClassDeclaration {

        @Nested
        class When_Analyzing {

            @Test
            void Then_CreatesClassNode() {
                var identNode = new ParsedNode("identifier", "UserService",
                        1, 1, 13, 24, List.of());
                var body = new ParsedNode("declaration_list", "{}",
                        2, 5, 0, 1, List.of());
                var classNode = new ParsedNode("class_declaration",
                        "public class UserService {}", 1, 5, 0, 1,
                        List.of(identNode, body));
                var tree = new ParsedTree("UserService.cs", "csharp",
                        "public class UserService {}", List.of(classNode));
                var graph = new KnowledgeGraph();

                analyzer.analyzeTree(tree, graph);

                assertThat(graph.nodesOfType(KgNodeType.CLASS)).hasSize(1);
                var classId = graph.nodesOfType(KgNodeType.CLASS).iterator().next();
                assertThat(graph.getNode(classId).name()).isEqualTo("UserService");
            }

            @Test
            void Then_CreatesContainsEdgeFromFileToClass() {
                var identNode = new ParsedNode("identifier", "UserService",
                        1, 1, 13, 24, List.of());
                var body = new ParsedNode("declaration_list", "{}",
                        2, 5, 0, 1, List.of());
                var classNode = new ParsedNode("class_declaration",
                        "public class UserService {}", 1, 5, 0, 1,
                        List.of(identNode, body));
                var tree = new ParsedTree("UserService.cs", "csharp",
                        "public class UserService {}", List.of(classNode));
                var graph = new KnowledgeGraph();

                analyzer.analyzeTree(tree, graph);

                var containsEdges = graph.edgesOfType(KgEdgeType.CONTAINS);
                assertThat(containsEdges).anyMatch(e ->
                        e.targetNodeId().contains("UserService"));
            }
        }
    }

    @Nested
    class Given_CSharpTreeWithInterfaceDeclaration {

        @Nested
        class When_Analyzing {

            @Test
            void Then_CreatesInterfaceNode() {
                var identNode = new ParsedNode("identifier", "IRepository",
                        1, 1, 17, 28, List.of());
                var body = new ParsedNode("declaration_list", "{}",
                        2, 3, 0, 1, List.of());
                var ifaceNode = new ParsedNode("interface_declaration",
                        "public interface IRepository {}", 1, 3, 0, 1,
                        List.of(identNode, body));
                var tree = new ParsedTree("IRepository.cs", "csharp",
                        "public interface IRepository {}", List.of(ifaceNode));
                var graph = new KnowledgeGraph();

                analyzer.analyzeTree(tree, graph);

                assertThat(graph.nodesOfType(KgNodeType.INTERFACE)).hasSize(1);
                var ifaceId = graph.nodesOfType(KgNodeType.INTERFACE).iterator().next();
                assertThat(graph.getNode(ifaceId).name()).isEqualTo("IRepository");
            }
        }
    }

    @Nested
    class Given_CSharpTreeWithStructDeclaration {

        @Nested
        class When_Analyzing {

            @Test
            void Then_CreatesStructNode() {
                var identNode = new ParsedNode("identifier", "Point",
                        1, 1, 14, 19, List.of());
                var body = new ParsedNode("declaration_list", "{}",
                        2, 3, 0, 1, List.of());
                var structNode = new ParsedNode("struct_declaration",
                        "public struct Point {}", 1, 3, 0, 1,
                        List.of(identNode, body));
                var tree = new ParsedTree("Point.cs", "csharp",
                        "public struct Point {}", List.of(structNode));
                var graph = new KnowledgeGraph();

                analyzer.analyzeTree(tree, graph);

                assertThat(graph.nodesOfType(KgNodeType.STRUCT)).hasSize(1);
                var structId = graph.nodesOfType(KgNodeType.STRUCT).iterator().next();
                assertThat(graph.getNode(structId).name()).isEqualTo("Point");
            }
        }
    }

    @Nested
    class Given_CSharpTreeWithEnumDeclaration {

        @Nested
        class When_Analyzing {

            @Test
            void Then_CreatesEnumNode() {
                var identNode = new ParsedNode("identifier", "Color",
                        1, 1, 12, 17, List.of());
                var body = new ParsedNode("enum_member_declaration_list", "{ Red, Green, Blue }",
                        1, 1, 18, 38, List.of());
                var enumNode = new ParsedNode("enum_declaration",
                        "public enum Color { Red, Green, Blue }", 1, 1, 0, 38,
                        List.of(identNode, body));
                var tree = new ParsedTree("Color.cs", "csharp",
                        "public enum Color { Red, Green, Blue }", List.of(enumNode));
                var graph = new KnowledgeGraph();

                analyzer.analyzeTree(tree, graph);

                assertThat(graph.nodesOfType(KgNodeType.ENUM)).hasSize(1);
                var enumId = graph.nodesOfType(KgNodeType.ENUM).iterator().next();
                assertThat(graph.getNode(enumId).name()).isEqualTo("Color");
            }
        }
    }

    @Nested
    class Given_CSharpTreeWithMethodDeclaration {

        @Nested
        class When_Analyzing {

            @Test
            void Then_CreatesMethodNode() {
                var methodIdent = new ParsedNode("identifier", "Calculate",
                        3, 3, 19, 28, List.of());
                var paramList = new ParsedNode("parameter_list", "(int x)",
                        3, 3, 28, 35, List.of());
                var block = new ParsedNode("block", "{ return x; }",
                        3, 3, 36, 49, List.of());
                var methodNode = new ParsedNode("method_declaration",
                        "public int Calculate(int x) { return x; }", 3, 3, 0, 49,
                        List.of(methodIdent, paramList, block));

                var classIdent = new ParsedNode("identifier", "Calculator",
                        1, 1, 13, 23, List.of());
                var body = new ParsedNode("declaration_list", "{ ... }",
                        2, 5, 0, 1, List.of(methodNode));
                var classNode = new ParsedNode("class_declaration",
                        "public class Calculator { ... }", 1, 5, 0, 1,
                        List.of(classIdent, body));
                var tree = new ParsedTree("Calculator.cs", "csharp",
                        "...", List.of(classNode));
                var graph = new KnowledgeGraph();

                analyzer.analyzeTree(tree, graph);

                assertThat(graph.nodesOfType(KgNodeType.METHOD)).hasSize(1);
                var methodId = graph.nodesOfType(KgNodeType.METHOD).iterator().next();
                assertThat(graph.getNode(methodId).name()).isEqualTo("Calculate");
            }

            @Test
            void Then_CreatesContainsEdgeFromClassToMethod() {
                var methodIdent = new ParsedNode("identifier", "Calculate",
                        3, 3, 19, 28, List.of());
                var methodNode = new ParsedNode("method_declaration",
                        "public int Calculate(int x) { return x; }", 3, 3, 0, 49,
                        List.of(methodIdent));

                var classIdent = new ParsedNode("identifier", "Calculator",
                        1, 1, 13, 23, List.of());
                var body = new ParsedNode("declaration_list", "{ ... }",
                        2, 5, 0, 1, List.of(methodNode));
                var classNode = new ParsedNode("class_declaration",
                        "public class Calculator { ... }", 1, 5, 0, 1,
                        List.of(classIdent, body));
                var tree = new ParsedTree("Calculator.cs", "csharp",
                        "...", List.of(classNode));
                var graph = new KnowledgeGraph();

                analyzer.analyzeTree(tree, graph);

                var containsEdges = graph.edgesOfType(KgEdgeType.CONTAINS);
                assertThat(containsEdges).anyMatch(e ->
                        e.sourceNodeId().contains("Calculator") &&
                                e.targetNodeId().contains("Calculate"));
            }
        }
    }

    @Nested
    class Given_CSharpTreeWithPropertyDeclaration {

        @Nested
        class When_Analyzing {

            @Test
            void Then_CreatesFieldNode() {
                var propIdent = new ParsedNode("identifier", "Name",
                        3, 3, 18, 22, List.of());
                var propType = new ParsedNode("predefined_type", "string",
                        3, 3, 11, 17, List.of());
                var propNode = new ParsedNode("property_declaration",
                        "public string Name { get; set; }", 3, 3, 0, 32,
                        List.of(propType, propIdent));

                var classIdent = new ParsedNode("identifier", "Person",
                        1, 1, 13, 19, List.of());
                var body = new ParsedNode("declaration_list", "{ ... }",
                        2, 5, 0, 1, List.of(propNode));
                var classNode = new ParsedNode("class_declaration",
                        "public class Person { ... }", 1, 5, 0, 1,
                        List.of(classIdent, body));
                var tree = new ParsedTree("Person.cs", "csharp",
                        "...", List.of(classNode));
                var graph = new KnowledgeGraph();

                analyzer.analyzeTree(tree, graph);

                assertThat(graph.nodesOfType(KgNodeType.FIELD)).hasSize(1);
                var fieldId = graph.nodesOfType(KgNodeType.FIELD).iterator().next();
                assertThat(graph.getNode(fieldId).name()).isEqualTo("Name");
            }
        }
    }

    @Nested
    class Given_CSharpTreeWithUsingDirective {

        @Nested
        class When_Analyzing {

            @Test
            void Then_CreatesImportsEdge() {
                var usingNode = new ParsedNode("using_directive",
                        "using System.Collections.Generic;", 1, 1, 0, 33, List.of());
                var tree = new ParsedTree("Program.cs", "csharp",
                        "using System.Collections.Generic;", List.of(usingNode));
                var graph = new KnowledgeGraph();

                analyzer.analyzeTree(tree, graph);

                var importEdges = graph.edgesOfType(KgEdgeType.IMPORTS);
                assertThat(importEdges).hasSize(1);
                assertThat(importEdges.get(0).label()).contains("System.Collections.Generic");
            }
        }
    }

    @Nested
    class Given_CSharpTreeWithClassExtendingBaseClass {

        @Nested
        class When_Analyzing {

            @Test
            void Then_CreatesExtendsEdge() {
                var identNode = new ParsedNode("identifier", "Dog",
                        1, 1, 13, 16, List.of());
                var baseType = new ParsedNode("identifier", "Animal",
                        1, 1, 19, 25, List.of());
                var baseList = new ParsedNode("base_list", ": Animal",
                        1, 1, 17, 25, List.of(baseType));
                var body = new ParsedNode("declaration_list", "{}",
                        2, 3, 0, 1, List.of());
                var classNode = new ParsedNode("class_declaration",
                        "public class Dog : Animal {}", 1, 3, 0, 1,
                        List.of(identNode, baseList, body));
                var tree = new ParsedTree("Dog.cs", "csharp",
                        "public class Dog : Animal {}", List.of(classNode));
                var graph = new KnowledgeGraph();

                analyzer.analyzeTree(tree, graph);

                var extendsEdges = graph.edgesOfType(KgEdgeType.EXTENDS);
                assertThat(extendsEdges).hasSize(1);
                assertThat(extendsEdges.get(0).label()).isEqualTo("Animal");
            }
        }
    }

    @Nested
    class Given_CSharpTreeWithClassImplementingInterface {

        @Nested
        class When_Analyzing {

            @Test
            void Then_CreatesImplementsEdge() {
                var identNode = new ParsedNode("identifier", "UserRepo",
                        1, 1, 13, 21, List.of());
                var ifaceType = new ParsedNode("identifier", "IRepository",
                        1, 1, 24, 35, List.of());
                var baseList = new ParsedNode("base_list", ": IRepository",
                        1, 1, 22, 35, List.of(ifaceType));
                var body = new ParsedNode("declaration_list", "{}",
                        2, 3, 0, 1, List.of());
                var classNode = new ParsedNode("class_declaration",
                        "public class UserRepo : IRepository {}", 1, 3, 0, 1,
                        List.of(identNode, baseList, body));
                var tree = new ParsedTree("UserRepo.cs", "csharp",
                        "public class UserRepo : IRepository {}", List.of(classNode));
                var graph = new KnowledgeGraph();

                analyzer.analyzeTree(tree, graph);

                var implementsEdges = graph.edgesOfType(KgEdgeType.IMPLEMENTS);
                assertThat(implementsEdges).hasSize(1);
                assertThat(implementsEdges.get(0).label()).isEqualTo("IRepository");
            }
        }
    }

    @Nested
    class Given_CSharpTreeWithExtensionMethod {

        @Nested
        class When_Analyzing {

            @Test
            void Then_CreatesExtensionFunctionNode() {
                var methodIdent = new ParsedNode("identifier", "ToUpper",
                        3, 3, 28, 35, List.of());
                var thisModifier = new ParsedNode("this", "this",
                        3, 3, 36, 40, List.of());
                var paramType = new ParsedNode("predefined_type", "string",
                        3, 3, 41, 47, List.of());
                var paramName = new ParsedNode("identifier", "s",
                        3, 3, 48, 49, List.of());
                var param = new ParsedNode("parameter", "this string s",
                        3, 3, 36, 49, List.of(thisModifier, paramType, paramName));
                var paramList = new ParsedNode("parameter_list", "(this string s)",
                        3, 3, 35, 50, List.of(param));
                var staticMod = new ParsedNode("static", "static",
                        3, 3, 7, 13, List.of());
                var modifiers = new ParsedNode("modifiers", "public static",
                        3, 3, 0, 13, List.of(staticMod));
                var block = new ParsedNode("block", "{ ... }",
                        3, 5, 51, 1, List.of());
                var methodNode = new ParsedNode("method_declaration",
                        "public static string ToUpper(this string s) { ... }", 3, 5, 0, 1,
                        List.of(modifiers, methodIdent, paramList, block));

                var classIdent = new ParsedNode("identifier", "StringExtensions",
                        1, 1, 21, 37, List.of());
                var classBody = new ParsedNode("declaration_list", "{ ... }",
                        2, 6, 0, 1, List.of(methodNode));
                var classNode = new ParsedNode("class_declaration",
                        "public static class StringExtensions { ... }", 1, 6, 0, 1,
                        List.of(classIdent, classBody));
                var tree = new ParsedTree("StringExtensions.cs", "csharp",
                        "...", List.of(classNode));
                var graph = new KnowledgeGraph();

                analyzer.analyzeTree(tree, graph);

                assertThat(graph.nodesOfType(KgNodeType.EXTENSION_FUNCTION)).hasSize(1);
                var extId = graph.nodesOfType(KgNodeType.EXTENSION_FUNCTION).iterator().next();
                assertThat(graph.getNode(extId).name()).isEqualTo("ToUpper");
            }
        }
    }

    @Nested
    class Given_CSharpTreeWithRecordDeclaration {

        @Nested
        class When_Analyzing {

            @Test
            void Then_CreatesRecordNode() {
                var identNode = new ParsedNode("identifier", "PersonRecord",
                        1, 1, 14, 26, List.of());
                var body = new ParsedNode("declaration_list", "{}",
                        2, 3, 0, 1, List.of());
                var recordNode = new ParsedNode("record_declaration",
                        "public record PersonRecord {}", 1, 3, 0, 1,
                        List.of(identNode, body));
                var tree = new ParsedTree("PersonRecord.cs", "csharp",
                        "public record PersonRecord {}", List.of(recordNode));
                var graph = new KnowledgeGraph();

                analyzer.analyzeTree(tree, graph);

                assertThat(graph.nodesOfType(KgNodeType.RECORD)).hasSize(1);
                var recordId = graph.nodesOfType(KgNodeType.RECORD).iterator().next();
                assertThat(graph.getNode(recordId).name()).isEqualTo("PersonRecord");
            }
        }
    }

    @Nested
    class Given_CSharpTreeWithConstructorDeclaration {

        @Nested
        class When_Analyzing {

            @Test
            void Then_CreatesConstructorNode() {
                var ctorIdent = new ParsedNode("identifier", "MyService",
                        3, 3, 11, 20, List.of());
                var paramList = new ParsedNode("parameter_list", "()",
                        3, 3, 20, 22, List.of());
                var block = new ParsedNode("block", "{ }",
                        3, 5, 23, 1, List.of());
                var ctorNode = new ParsedNode("constructor_declaration",
                        "public MyService() { }", 3, 5, 0, 1,
                        List.of(ctorIdent, paramList, block));

                var classIdent = new ParsedNode("identifier", "MyService",
                        1, 1, 13, 22, List.of());
                var body = new ParsedNode("declaration_list", "{ ... }",
                        2, 6, 0, 1, List.of(ctorNode));
                var classNode = new ParsedNode("class_declaration",
                        "public class MyService { ... }", 1, 6, 0, 1,
                        List.of(classIdent, body));
                var tree = new ParsedTree("MyService.cs", "csharp",
                        "...", List.of(classNode));
                var graph = new KnowledgeGraph();

                analyzer.analyzeTree(tree, graph);

                assertThat(graph.nodesOfType(KgNodeType.CONSTRUCTOR)).hasSize(1);
            }
        }
    }

    @Nested
    class Given_CSharpTreeWithFieldDeclaration {

        @Nested
        class When_Analyzing {

            @Test
            void Then_CreatesFieldNodeFromVariableDeclarator() {
                var varIdent = new ParsedNode("identifier", "_count",
                        3, 3, 16, 22, List.of());
                var varDeclarator = new ParsedNode("variable_declarator", "_count",
                        3, 3, 16, 22, List.of(varIdent));
                var fieldNode = new ParsedNode("field_declaration",
                        "private int _count;", 3, 3, 0, 19,
                        List.of(varDeclarator));

                var classIdent = new ParsedNode("identifier", "Counter",
                        1, 1, 13, 20, List.of());
                var body = new ParsedNode("declaration_list", "{ ... }",
                        2, 5, 0, 1, List.of(fieldNode));
                var classNode = new ParsedNode("class_declaration",
                        "public class Counter { ... }", 1, 5, 0, 1,
                        List.of(classIdent, body));
                var tree = new ParsedTree("Counter.cs", "csharp",
                        "...", List.of(classNode));
                var graph = new KnowledgeGraph();

                analyzer.analyzeTree(tree, graph);

                assertThat(graph.nodesOfType(KgNodeType.FIELD)).hasSize(1);
                var fieldId = graph.nodesOfType(KgNodeType.FIELD).iterator().next();
                assertThat(graph.getNode(fieldId).name()).isEqualTo("_count");
            }
        }
    }

    @Nested
    class Given_CSharpTreeWithClassInsideNamespace {

        @Nested
        class When_Analyzing {

            @Test
            void Then_QualifiedNameIncludesNamespace() {
                var classIdent = new ParsedNode("identifier", "UserService",
                        3, 3, 17, 28, List.of());
                var classBody = new ParsedNode("declaration_list", "{}",
                        4, 5, 0, 1, List.of());
                var classNode = new ParsedNode("class_declaration",
                        "public class UserService {}", 3, 5, 0, 1,
                        List.of(classIdent, classBody));

                var nsIdent = new ParsedNode("identifier", "MyApp.Services",
                        1, 1, 10, 24, List.of());
                var nsBody = new ParsedNode("declaration_list", "{ ... }",
                        2, 6, 0, 1, List.of(classNode));
                var nsNode = new ParsedNode("namespace_declaration",
                        "namespace MyApp.Services { ... }", 1, 6, 0, 1,
                        List.of(nsIdent, nsBody));
                var tree = new ParsedTree("UserService.cs", "csharp",
                        "...", List.of(nsNode));
                var graph = new KnowledgeGraph();

                analyzer.analyzeTree(tree, graph);

                var classId = graph.nodesOfType(KgNodeType.CLASS).iterator().next();
                assertThat(graph.getNode(classId).qualifiedName())
                        .isEqualTo("MyApp.Services.UserService");
            }
        }
    }

    @Nested
    class Given_EmptyTree {

        @Nested
        class When_Analyzing {

            @Test
            void Then_CreatesOnlyFileNode() {
                var tree = new ParsedTree("Empty.cs", "csharp", "", List.of());
                var graph = new KnowledgeGraph();

                analyzer.analyzeTree(tree, graph);

                assertThat(graph.nodesOfType(KgNodeType.FILE)).hasSize(1);
                assertThat(graph.nodesOfType(KgNodeType.CLASS)).isEmpty();
                assertThat(graph.nodesOfType(KgNodeType.METHOD)).isEmpty();
                assertThat(graph.nodesOfType(KgNodeType.FIELD)).isEmpty();
            }
        }
    }
}
