package com.codevision.codevisionbackend.analysis.multilang;

import com.codevision.codevisionbackend.graph.KgEdgeType;
import com.codevision.codevisionbackend.graph.KgNodeType;
import com.codevision.codevisionbackend.graph.KnowledgeGraph;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JavaTreeSitterAnalyzerTest {

    private final JavaTreeSitterAnalyzer analyzer = new JavaTreeSitterAnalyzer();

    @Nested
    class Given_JavaTreeWithPackageClassMethod {

        @Nested
        class When_Analyzing {

            @Test
            void Then_CreatesPackageNode() {
                var tree = javaTreeWithPackageClassMethod();
                var graph = new KnowledgeGraph();

                analyzer.analyzeTree(tree, graph);

                assertThat(graph.nodesOfType(KgNodeType.PACKAGE)).hasSize(1);
                var pkgId = graph.nodesOfType(KgNodeType.PACKAGE).iterator().next();
                assertThat(graph.getNode(pkgId).name()).isEqualTo("com.example");
            }

            @Test
            void Then_CreatesClassNode() {
                var tree = javaTreeWithPackageClassMethod();
                var graph = new KnowledgeGraph();

                analyzer.analyzeTree(tree, graph);

                assertThat(graph.nodesOfType(KgNodeType.CLASS)).hasSize(1);
                var classId = graph.nodesOfType(KgNodeType.CLASS).iterator().next();
                assertThat(graph.getNode(classId).qualifiedName()).isEqualTo("com.example.MyService");
            }

            @Test
            void Then_CreatesMethodNode() {
                var tree = javaTreeWithPackageClassMethod();
                var graph = new KnowledgeGraph();

                analyzer.analyzeTree(tree, graph);

                assertThat(graph.nodesOfType(KgNodeType.METHOD)).hasSize(1);
                var methodId = graph.nodesOfType(KgNodeType.METHOD).iterator().next();
                assertThat(graph.getNode(methodId).name()).isEqualTo("process");
            }

            @Test
            void Then_CreatesContainmentHierarchy() {
                var tree = javaTreeWithPackageClassMethod();
                var graph = new KnowledgeGraph();

                analyzer.analyzeTree(tree, graph);

                // Package -> File
                var pkgId = graph.nodesOfType(KgNodeType.PACKAGE).iterator().next();
                var pkgEdges = graph.getNeighbors(pkgId);
                assertThat(pkgEdges).anyMatch(e -> e.type() == KgEdgeType.CONTAINS);

                // File -> Class
                var fileId = graph.nodesOfType(KgNodeType.FILE).iterator().next();
                var fileEdges = graph.getNeighbors(fileId);
                assertThat(fileEdges).anyMatch(e -> e.type() == KgEdgeType.CONTAINS);
            }
        }
    }

    @Nested
    class Given_JavaTreeWithImports {

        @Nested
        class When_Analyzing {

            @Test
            void Then_CreatesImportsEdges() {
                var importNode = new ParsedNode("import_declaration",
                        "import java.util.List;", 2, 2, 0, 22, List.of());
                var tree = new ParsedTree("Test.java", "java", "source",
                        List.of(importNode));
                var graph = new KnowledgeGraph();

                analyzer.analyzeTree(tree, graph);

                var importsEdges = graph.edgesOfType(KgEdgeType.IMPORTS);
                assertThat(importsEdges).hasSize(1);
                assertThat(importsEdges.get(0).label()).contains("java.util.List");
            }
        }
    }

    @Nested
    class Given_JavaTreeWithInterface {

        @Nested
        class When_Analyzing {

            @Test
            void Then_CreatesInterfaceNode() {
                var ifaceNode = new ParsedNode("interface_declaration",
                        "public interface Repository", 1, 10, 0, 1, List.of(
                        new ParsedNode("identifier", "Repository", 1, 1, 17, 27, List.of()),
                        new ParsedNode("interface_body", "{}", 1, 10, 28, 1, List.of())
                ));
                var tree = new ParsedTree("Repository.java", "java", "source",
                        List.of(ifaceNode));
                var graph = new KnowledgeGraph();

                analyzer.analyzeTree(tree, graph);

                assertThat(graph.nodesOfType(KgNodeType.INTERFACE)).hasSize(1);
            }
        }
    }

    @Nested
    class Given_JavaTreeWithEnum {

        @Nested
        class When_Analyzing {

            @Test
            void Then_CreatesEnumNode() {
                var enumNode = new ParsedNode("enum_declaration",
                        "public enum Status", 1, 5, 0, 1, List.of(
                        new ParsedNode("identifier", "Status", 1, 1, 12, 18, List.of()),
                        new ParsedNode("enum_body", "{ ACTIVE, INACTIVE }", 1, 5, 19, 1, List.of())
                ));
                var tree = new ParsedTree("Status.java", "java", "source",
                        List.of(enumNode));
                var graph = new KnowledgeGraph();

                analyzer.analyzeTree(tree, graph);

                assertThat(graph.nodesOfType(KgNodeType.ENUM)).hasSize(1);
                var enumId = graph.nodesOfType(KgNodeType.ENUM).iterator().next();
                assertThat(graph.getNode(enumId).name()).isEqualTo("Status");
            }
        }
    }

    @Nested
    class Given_JavaTreeWithFieldsAndConstructor {

        @Nested
        class When_Analyzing {

            @Test
            void Then_CreatesFieldAndConstructorNodes() {
                var fieldNode = new ParsedNode("field_declaration",
                        "private String name;", 3, 3, 4, 24, List.of(
                        new ParsedNode("variable_declarator", "name", 3, 3, 19, 23, List.of(
                                new ParsedNode("identifier", "name", 3, 3, 19, 23, List.of())
                        ))
                ));
                var ctorNode = new ParsedNode("constructor_declaration",
                        "public MyService(String name)", 5, 8, 4, 5, List.of(
                        new ParsedNode("identifier", "MyService", 5, 5, 11, 20, List.of())
                ));
                var classBody = new ParsedNode("class_body", "{ ... }", 2, 10, 0, 1,
                        List.of(fieldNode, ctorNode));
                var classNode = new ParsedNode("class_declaration",
                        "public class MyService", 1, 10, 0, 1, List.of(
                        new ParsedNode("identifier", "MyService", 1, 1, 13, 22, List.of()),
                        classBody
                ));
                var tree = new ParsedTree("MyService.java", "java", "source",
                        List.of(classNode));
                var graph = new KnowledgeGraph();

                analyzer.analyzeTree(tree, graph);

                assertThat(graph.nodesOfType(KgNodeType.FIELD)).hasSize(1);
                assertThat(graph.nodesOfType(KgNodeType.CONSTRUCTOR)).hasSize(1);
            }
        }
    }

    @Nested
    class Given_JavaTreeWithSuperclass {

        @Nested
        class When_Analyzing {

            @Test
            void Then_CreatesExtendsEdge() {
                var classNode = new ParsedNode("class_declaration",
                        "public class Dog extends Animal", 1, 10, 0, 1, List.of(
                        new ParsedNode("identifier", "Dog", 1, 1, 13, 16, List.of()),
                        new ParsedNode("superclass", "Animal", 1, 1, 25, 31, List.of(
                                new ParsedNode("type_identifier", "Animal", 1, 1, 25, 31, List.of())
                        )),
                        new ParsedNode("class_body", "{}", 2, 10, 0, 1, List.of())
                ));
                var tree = new ParsedTree("Dog.java", "java", "source",
                        List.of(classNode));
                var graph = new KnowledgeGraph();

                analyzer.analyzeTree(tree, graph);

                var extendsEdges = graph.edgesOfType(KgEdgeType.EXTENDS);
                assertThat(extendsEdges).hasSize(1);
                assertThat(extendsEdges.get(0).label()).isEqualTo("Animal");
            }
        }
    }

    @Nested
    class Given_JavaTreeWithAnnotationDeclaration {

        @Nested
        class When_Analyzing {

            @Test
            void Then_CreatesAnnotationTypeNode() {
                var annotationNode = new ParsedNode("annotation_declaration",
                        "public @interface MyAnnotation", 1, 5, 0, 1, List.of(
                        new ParsedNode("identifier", "MyAnnotation", 1, 1, 18, 30, List.of()),
                        new ParsedNode("annotation_type_body", "{}", 1, 5, 31, 1, List.of())
                ));
                var tree = new ParsedTree("MyAnnotation.java", "java", "source",
                        List.of(annotationNode));
                var graph = new KnowledgeGraph();

                analyzer.analyzeTree(tree, graph);

                assertThat(graph.nodesOfType(KgNodeType.ANNOTATION_TYPE)).hasSize(1);
                var annotId = graph.nodesOfType(KgNodeType.ANNOTATION_TYPE).iterator().next();
                assertThat(graph.getNode(annotId).name()).isEqualTo("MyAnnotation");
            }

            @Test
            void Then_CreatesContainsEdgeFromFileToAnnotationType() {
                var annotationNode = new ParsedNode("annotation_declaration",
                        "public @interface MyAnnotation", 1, 5, 0, 1, List.of(
                        new ParsedNode("identifier", "MyAnnotation", 1, 1, 18, 30, List.of()),
                        new ParsedNode("annotation_type_body", "{}", 1, 5, 31, 1, List.of())
                ));
                var tree = new ParsedTree("MyAnnotation.java", "java", "source",
                        List.of(annotationNode));
                var graph = new KnowledgeGraph();

                analyzer.analyzeTree(tree, graph);

                var containsEdges = graph.edgesOfType(KgEdgeType.CONTAINS);
                assertThat(containsEdges).anyMatch(e ->
                        e.targetNodeId().contains("MyAnnotation"));
            }
        }
    }

    @Nested
    class Given_JavaTreeWithClassImplementingInterfaces {

        @Nested
        class When_Analyzing {

            @Test
            void Then_CreatesImplementsEdge() {
                var classNode = new ParsedNode("class_declaration",
                        "public class MyService implements Runnable", 1, 10, 0, 1, List.of(
                        new ParsedNode("identifier", "MyService", 1, 1, 13, 22, List.of()),
                        new ParsedNode("super_interfaces", "implements Runnable", 1, 1, 23, 42, List.of(
                                new ParsedNode("type_identifier", "Runnable", 1, 1, 34, 42, List.of())
                        )),
                        new ParsedNode("class_body", "{}", 2, 10, 0, 1, List.of())
                ));
                var tree = new ParsedTree("MyService.java", "java", "source",
                        List.of(classNode));
                var graph = new KnowledgeGraph();

                analyzer.analyzeTree(tree, graph);

                var implementsEdges = graph.edgesOfType(KgEdgeType.IMPLEMENTS);
                assertThat(implementsEdges).hasSize(1);
                assertThat(implementsEdges.get(0).label()).isEqualTo("Runnable");
            }

            @Test
            void Then_CreatesMultipleImplementsEdgesForMultipleInterfaces() {
                var classNode = new ParsedNode("class_declaration",
                        "public class MyService implements Runnable, Serializable", 1, 10, 0, 1, List.of(
                        new ParsedNode("identifier", "MyService", 1, 1, 13, 22, List.of()),
                        new ParsedNode("super_interfaces", "implements Runnable, Serializable", 1, 1, 23, 56, List.of(
                                new ParsedNode("type_identifier", "Runnable", 1, 1, 34, 42, List.of()),
                                new ParsedNode("type_identifier", "Serializable", 1, 1, 44, 56, List.of())
                        )),
                        new ParsedNode("class_body", "{}", 2, 10, 0, 1, List.of())
                ));
                var tree = new ParsedTree("MyService.java", "java", "source",
                        List.of(classNode));
                var graph = new KnowledgeGraph();

                analyzer.analyzeTree(tree, graph);

                var implementsEdges = graph.edgesOfType(KgEdgeType.IMPLEMENTS);
                assertThat(implementsEdges).hasSize(2);
                assertThat(implementsEdges).anyMatch(e -> e.label().equals("Runnable"));
                assertThat(implementsEdges).anyMatch(e -> e.label().equals("Serializable"));
            }
        }
    }

    // ── Fixtures ────────────────────────────────────────────────────────

    private ParsedTree javaTreeWithPackageClassMethod() {
        var packageNode = new ParsedNode("package_declaration",
                "package com.example;", 1, 1, 0, 20, List.of(
                new ParsedNode("scoped_identifier", "com.example", 1, 1, 8, 19, List.of())
        ));
        var methodNode = new ParsedNode("method_declaration",
                "public void process()", 4, 8, 4, 5, List.of(
                new ParsedNode("identifier", "process", 4, 4, 17, 24, List.of())
        ));
        var classBody = new ParsedNode("class_body", "{ void process() {} }",
                3, 9, 0, 1, List.of(methodNode));
        var classNode = new ParsedNode("class_declaration",
                "public class MyService", 3, 9, 0, 1, List.of(
                new ParsedNode("identifier", "MyService", 3, 3, 13, 22, List.of()),
                classBody
        ));
        return new ParsedTree("MyService.java", "java", "package com.example;\n\npublic class MyService {\n  public void process() {}\n}",
                List.of(packageNode, classNode));
    }
}
