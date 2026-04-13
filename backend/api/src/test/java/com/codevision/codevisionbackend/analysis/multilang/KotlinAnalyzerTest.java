package com.codevision.codevisionbackend.analysis.multilang;

import com.codevision.codevisionbackend.graph.KgEdgeType;
import com.codevision.codevisionbackend.graph.KgNodeType;
import com.codevision.codevisionbackend.graph.KnowledgeGraph;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class KotlinAnalyzerTest {

    private final KotlinAnalyzer analyzer = new KotlinAnalyzer();

    @Nested
    class Given_KotlinTreeWithPackageHeader {

        @Nested
        class When_Analyzing {

            @Test
            void Then_CreatesPackageNode() {
                var identNode = new ParsedNode("identifier", "com.example.app",
                        1, 1, 8, 23, List.of());
                var packageHeader = new ParsedNode("package_header",
                        "package com.example.app", 1, 1, 0, 23,
                        List.of(identNode));
                var tree = new ParsedTree("Main.kt", "kotlin",
                        "package com.example.app", List.of(packageHeader));
                var graph = new KnowledgeGraph();

                analyzer.analyzeTree(tree, graph);

                assertThat(graph.nodesOfType(KgNodeType.PACKAGE)).hasSize(1);
                var pkgId = graph.nodesOfType(KgNodeType.PACKAGE).iterator().next();
                assertThat(graph.getNode(pkgId).name()).isEqualTo("com.example.app");
            }

            @Test
            void Then_CreatesContainsEdgeFromPackageToFile() {
                var identNode = new ParsedNode("identifier", "com.example.app",
                        1, 1, 8, 23, List.of());
                var packageHeader = new ParsedNode("package_header",
                        "package com.example.app", 1, 1, 0, 23,
                        List.of(identNode));
                var tree = new ParsedTree("Main.kt", "kotlin",
                        "package com.example.app", List.of(packageHeader));
                var graph = new KnowledgeGraph();

                analyzer.analyzeTree(tree, graph);

                var containsEdges = graph.edgesOfType(KgEdgeType.CONTAINS);
                assertThat(containsEdges).anyMatch(e ->
                        e.sourceNodeId().contains("com.example.app") && e.targetNodeId().contains("Main.kt"));
            }
        }
    }

    @Nested
    class Given_KotlinTreeWithDataClass {

        @Nested
        class When_Analyzing {

            @Test
            void Then_CreatesClassNodeWithDataClassMetadata() {
                var identNode = new ParsedNode("identifier", "User",
                        1, 1, 11, 15, List.of());
                var classBody = new ParsedNode("class_body", "{}",
                        1, 3, 16, 1, List.of());
                var classNode = new ParsedNode("class_declaration",
                        "data class User {}", 1, 3, 0, 1,
                        List.of(identNode, classBody));
                var tree = new ParsedTree("User.kt", "kotlin",
                        "data class User {}", List.of(classNode));
                var graph = new KnowledgeGraph();

                analyzer.analyzeTree(tree, graph);

                assertThat(graph.nodesOfType(KgNodeType.CLASS)).hasSize(1);
                var classId = graph.nodesOfType(KgNodeType.CLASS).iterator().next();
                var node = graph.getNode(classId);
                assertThat(node.name()).isEqualTo("User");
                assertThat(node.metadata().languageSpecific())
                        .containsEntry("isDataClass", true);
            }

            @Test
            void Then_NonDataClassDoesNotHaveDataClassMetadata() {
                var identNode = new ParsedNode("identifier", "Service",
                        1, 1, 6, 13, List.of());
                var classBody = new ParsedNode("class_body", "{}",
                        1, 3, 14, 1, List.of());
                var classNode = new ParsedNode("class_declaration",
                        "class Service {}", 1, 3, 0, 1,
                        List.of(identNode, classBody));
                var tree = new ParsedTree("Service.kt", "kotlin",
                        "class Service {}", List.of(classNode));
                var graph = new KnowledgeGraph();

                analyzer.analyzeTree(tree, graph);

                assertThat(graph.nodesOfType(KgNodeType.CLASS)).hasSize(1);
                var classId = graph.nodesOfType(KgNodeType.CLASS).iterator().next();
                var node = graph.getNode(classId);
                assertThat(node.metadata().languageSpecific())
                        .doesNotContainKey("isDataClass");
            }
        }
    }

    @Nested
    class Given_KotlinTreeWithClassDeclaration {

        @Nested
        class When_Analyzing {

            @Test
            void Then_CreatesClassNode() {
                var identNode = new ParsedNode("identifier", "UserService",
                        1, 1, 6, 17, List.of());
                var classBody = new ParsedNode("class_body", "{}",
                        1, 3, 18, 1, List.of());
                var classNode = new ParsedNode("class_declaration",
                        "class UserService {}", 1, 3, 0, 1,
                        List.of(identNode, classBody));
                var tree = new ParsedTree("UserService.kt", "kotlin",
                        "class UserService {}", List.of(classNode));
                var graph = new KnowledgeGraph();

                analyzer.analyzeTree(tree, graph);

                assertThat(graph.nodesOfType(KgNodeType.CLASS)).hasSize(1);
                var classId = graph.nodesOfType(KgNodeType.CLASS).iterator().next();
                assertThat(graph.getNode(classId).name()).isEqualTo("UserService");
            }

            @Test
            void Then_CreatesContainsEdgeFromFileToClass() {
                var identNode = new ParsedNode("identifier", "UserService",
                        1, 1, 6, 17, List.of());
                var classBody = new ParsedNode("class_body", "{}",
                        1, 3, 18, 1, List.of());
                var classNode = new ParsedNode("class_declaration",
                        "class UserService {}", 1, 3, 0, 1,
                        List.of(identNode, classBody));
                var tree = new ParsedTree("UserService.kt", "kotlin",
                        "class UserService {}", List.of(classNode));
                var graph = new KnowledgeGraph();

                analyzer.analyzeTree(tree, graph);

                var containsEdges = graph.edgesOfType(KgEdgeType.CONTAINS);
                assertThat(containsEdges).anyMatch(e ->
                        e.targetNodeId().contains("UserService"));
            }
        }
    }

    @Nested
    class Given_KotlinTreeWithObjectDeclaration {

        @Nested
        class When_Analyzing {

            @Test
            void Then_CreatesClassNodeWithSingletonMetadata() {
                var identNode = new ParsedNode("identifier", "AppConfig",
                        1, 1, 7, 16, List.of());
                var classBody = new ParsedNode("class_body", "{}",
                        1, 3, 17, 1, List.of());
                var objectNode = new ParsedNode("object_declaration",
                        "object AppConfig {}", 1, 3, 0, 1,
                        List.of(identNode, classBody));
                var tree = new ParsedTree("AppConfig.kt", "kotlin",
                        "object AppConfig {}", List.of(objectNode));
                var graph = new KnowledgeGraph();

                analyzer.analyzeTree(tree, graph);

                assertThat(graph.nodesOfType(KgNodeType.CLASS)).hasSize(1);
                var classId = graph.nodesOfType(KgNodeType.CLASS).iterator().next();
                var node = graph.getNode(classId);
                assertThat(node.name()).isEqualTo("AppConfig");
                assertThat(node.metadata().languageSpecific())
                        .containsEntry("isSingleton", true);
            }
        }
    }

    @Nested
    class Given_KotlinTreeWithFunctionDeclaration {

        @Nested
        class When_Analyzing {

            @Test
            void Then_CreatesMethodNode() {
                var identNode = new ParsedNode("identifier", "greet",
                        1, 1, 4, 9, List.of());
                var funcNode = new ParsedNode("function_declaration",
                        "fun greet() {}", 1, 1, 0, 14,
                        List.of(identNode));
                var tree = new ParsedTree("Main.kt", "kotlin",
                        "fun greet() {}", List.of(funcNode));
                var graph = new KnowledgeGraph();

                analyzer.analyzeTree(tree, graph);

                assertThat(graph.nodesOfType(KgNodeType.METHOD)).hasSize(1);
                var methodId = graph.nodesOfType(KgNodeType.METHOD).iterator().next();
                assertThat(graph.getNode(methodId).name()).isEqualTo("greet");
            }
        }
    }

    @Nested
    class Given_KotlinTreeWithInterfaceDeclaration {

        @Nested
        class When_Analyzing {

            @Test
            void Then_CreatesInterfaceNode() {
                var identNode = new ParsedNode("identifier", "Repository",
                        1, 1, 10, 20, List.of());
                var classBody = new ParsedNode("class_body", "{}",
                        1, 3, 21, 1, List.of());
                var ifaceNode = new ParsedNode("interface_declaration",
                        "interface Repository {}", 1, 3, 0, 1,
                        List.of(identNode, classBody));
                var tree = new ParsedTree("Repository.kt", "kotlin",
                        "interface Repository {}", List.of(ifaceNode));
                var graph = new KnowledgeGraph();

                analyzer.analyzeTree(tree, graph);

                assertThat(graph.nodesOfType(KgNodeType.INTERFACE)).hasSize(1);
                var ifaceId = graph.nodesOfType(KgNodeType.INTERFACE).iterator().next();
                assertThat(graph.getNode(ifaceId).name()).isEqualTo("Repository");
            }
        }
    }
}
