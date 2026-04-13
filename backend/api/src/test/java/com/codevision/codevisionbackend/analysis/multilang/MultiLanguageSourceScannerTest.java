package com.codevision.codevisionbackend.analysis.multilang;

import com.codevision.codevisionbackend.config.AnalysisSafetyProperties;
import com.codevision.codevisionbackend.graph.KgEdgeType;
import com.codevision.codevisionbackend.graph.KgNodeType;
import com.codevision.codevisionbackend.graph.KnowledgeGraph;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

class MultiLanguageSourceScannerTest {

    private LanguageRegistry registry;
    private GenericTreeSitterAnalyzer genericAnalyzer;
    private JavaTreeSitterAnalyzer javaAnalyzer;
    private PythonAnalyzer pythonAnalyzer;
    private TypeScriptAnalyzer tsAnalyzer;

    @BeforeEach
    void setUp() {
        registry = new LanguageRegistry();
        genericAnalyzer = new GenericTreeSitterAnalyzer();
        javaAnalyzer = new JavaTreeSitterAnalyzer();
        pythonAnalyzer = new PythonAnalyzer();
        tsAnalyzer = new TypeScriptAnalyzer();
    }

    @Nested
    class Given_JavaAndPythonParsedTrees {

        @Nested
        class When_AnalyzingBoth {

            @Test
            void Then_CreatesNodesFromBothLanguages() {
                var javaPkg = new ParsedNode("package_declaration",
                        "package com.app;", 1, 1, 0, 16, List.of(
                        new ParsedNode("scoped_identifier", "com.app", 1, 1, 8, 15, List.of())
                ));
                var javaClass = new ParsedNode("class_declaration",
                        "public class App", 3, 8, 0, 1, List.of(
                        new ParsedNode("identifier", "App", 3, 3, 13, 16, List.of()),
                        new ParsedNode("class_body", "{}", 3, 8, 17, 1, List.of())
                ));
                var javaTree = new ParsedTree("App.java", "java", "source",
                        List.of(javaPkg, javaClass));

                var pyClass = new ParsedNode("class_definition",
                        "class Handler", 1, 6, 0, 1, List.of(
                        new ParsedNode("identifier", "Handler", 1, 1, 6, 13, List.of()),
                        new ParsedNode("block", "pass", 2, 6, 4, 8, List.of())
                ));
                var pyFunc = new ParsedNode("function_definition",
                        "def main()", 8, 10, 0, 1, List.of(
                        new ParsedNode("identifier", "main", 8, 8, 4, 8, List.of())
                ));
                var pyTree = new ParsedTree("handler.py", "python", "source",
                        List.of(pyClass, pyFunc));

                var graph = new KnowledgeGraph();

                // Simulate what the scanner does: dispatch to the right analyzer
                javaAnalyzer.analyzeTree(javaTree, graph);
                pythonAnalyzer.analyzeTree(pyTree, graph);

                // Should have nodes from both languages
                assertThat(graph.nodesOfType(KgNodeType.FILE)).hasSize(2);
                assertThat(graph.nodesOfType(KgNodeType.CLASS)).hasSize(2); // App + Handler
                assertThat(graph.nodesOfType(KgNodeType.METHOD)).hasSize(1); // main
                assertThat(graph.nodesOfType(KgNodeType.PACKAGE)).hasSize(1); // com.app
            }
        }
    }

    @Nested
    class Given_UnknownLanguageParsedTree {

        @Nested
        class When_AnalyzingWithGenericFallback {

            @Test
            void Then_GenericAnalyzerHandlesIt() {
                var funcNode = new ParsedNode("function_definition",
                        "func doStuff()", 1, 5, 0, 1, List.of(
                        new ParsedNode("identifier", "doStuff", 1, 1, 5, 12, List.of())
                ));
                var tree = new ParsedTree("script.lua", "lua", "source",
                        List.of(funcNode));
                var graph = new KnowledgeGraph();

                genericAnalyzer.analyzeGeneric(tree, graph);

                assertThat(graph.nodesOfType(KgNodeType.FILE)).hasSize(1);
                assertThat(graph.nodesOfType(KgNodeType.METHOD)).hasSize(1);
            }
        }
    }

    @Nested
    class Given_MultipleTreesFromDifferentLanguages {

        @Nested
        class When_AnalyzingAll {

            @Test
            void Then_AggregatesResultsIntoSingleGraph() {
                var javaClass = new ParsedNode("class_declaration",
                        "class Service", 1, 5, 0, 1, List.of(
                        new ParsedNode("identifier", "Service", 1, 1, 6, 13, List.of()),
                        new ParsedNode("class_body", "{}", 1, 5, 14, 1, List.of())
                ));
                var javaTree = new ParsedTree("Service.java", "java", "source",
                        List.of(javaClass));

                var tsFunc = new ParsedNode("function_declaration",
                        "function render()", 1, 3, 0, 1, List.of(
                        new ParsedNode("identifier", "render", 1, 1, 9, 15, List.of())
                ));
                var tsTree = new ParsedTree("app.ts", "typescript", "source",
                        List.of(tsFunc));

                var pyFunc = new ParsedNode("function_definition",
                        "def process()", 1, 3, 0, 1, List.of(
                        new ParsedNode("identifier", "process", 1, 1, 4, 11, List.of())
                ));
                var pyTree = new ParsedTree("main.py", "python", "source",
                        List.of(pyFunc));

                var graph = new KnowledgeGraph();

                javaAnalyzer.analyzeTree(javaTree, graph);
                tsAnalyzer.analyzeTree(tsTree, graph);
                pythonAnalyzer.analyzeTree(pyTree, graph);

                // 3 file nodes, 1 class, 2 methods
                assertThat(graph.nodesOfType(KgNodeType.FILE)).hasSize(3);
                assertThat(graph.nodesOfType(KgNodeType.CLASS)).hasSize(1);
                assertThat(graph.nodesOfType(KgNodeType.METHOD)).hasSize(2);
            }
        }
    }

    @Nested
    class Given_NullTree {

        @Nested
        class When_Analyzing {

            @Test
            void Then_DoesNotThrow() {
                var graph = new KnowledgeGraph();

                genericAnalyzer.analyzeGeneric(null, graph);

                assertThat(graph.nodeCount()).isEqualTo(0);
            }
        }
    }

    @Nested
    class Given_TreeWithImportsFromMultipleLanguages {

        @Nested
        class When_Analyzing {

            @Test
            void Then_CreatesImportsEdgesForEachLanguage() {
                var javaImport = new ParsedNode("import_declaration",
                        "import java.util.List;", 1, 1, 0, 22, List.of());
                var javaTree = new ParsedTree("App.java", "java", "source",
                        List.of(javaImport));

                var pyImport = new ParsedNode("import_from_statement",
                        "from os import path", 1, 1, 0, 19, List.of());
                var pyTree = new ParsedTree("util.py", "python", "source",
                        List.of(pyImport));

                var graph = new KnowledgeGraph();

                javaAnalyzer.analyzeTree(javaTree, graph);
                pythonAnalyzer.analyzeTree(pyTree, graph);

                var importsEdges = graph.edgesOfType(KgEdgeType.IMPORTS);
                assertThat(importsEdges).hasSize(2);
            }
        }
    }

    // ── CRITICAL 1: scanDirectory() tests ───────────────────────────────

    @Nested
    class Given_DirectoryWithSourceFiles {

        @TempDir
        Path tempDir;

        @Nested
        class When_ScanningDirectory {

            @Test
            void Then_ParsesAndAnalyzesAllSupportedFiles() throws IOException {
                // arrange – create a .java and .py file in the temp directory
                Files.writeString(tempDir.resolve("App.java"),
                        "public class App {}");
                Files.writeString(tempDir.resolve("main.py"),
                        "def hello():\n    pass");
                // unsupported file should be ignored
                Files.writeString(tempDir.resolve("readme.txt"),
                        "just a readme");

                var reg = new LanguageRegistry();
                var bridge = new TreeSitterBridge(reg);
                var analyzers = List.<LanguageAnalyzer>of(
                        new JavaTreeSitterAnalyzer(),
                        new PythonAnalyzer());
                var scanner = new MultiLanguageSourceScanner(bridge, analyzers, reg, new AnalysisSafetyProperties());

                // act
                var graph = scanner.scanDirectory(tempDir);

                // assert – each language-specific analyzer creates a FILE node
                assertThat(graph.nodesOfType(KgNodeType.FILE)).hasSizeGreaterThanOrEqualTo(2);
            }
        }
    }

    @Nested
    class Given_DirectoryWithSubdirectories {

        @TempDir
        Path tempDir;

        @Nested
        class When_ScanningDirectory {

            @Test
            void Then_RecursivelyScansSubdirectories() throws IOException {
                var subDir = tempDir.resolve("src");
                Files.createDirectory(subDir);
                Files.writeString(subDir.resolve("Service.java"),
                        "public class Service {}");
                Files.writeString(tempDir.resolve("app.py"),
                        "class Handler:\n    pass");

                var reg = new LanguageRegistry();
                var bridge = new TreeSitterBridge(reg);
                var analyzers = List.<LanguageAnalyzer>of(
                        new JavaTreeSitterAnalyzer(),
                        new PythonAnalyzer());
                var scanner = new MultiLanguageSourceScanner(bridge, analyzers, reg, new AnalysisSafetyProperties());

                var graph = scanner.scanDirectory(tempDir);

                // Both files should be found (one in root, one in subdir)
                assertThat(graph.nodesOfType(KgNodeType.FILE)).hasSizeGreaterThanOrEqualTo(2);
            }
        }
    }

    @Nested
    class Given_ZeroMaxRuntimeSeconds {

        @TempDir
        Path tempDir;

        @Nested
        class When_ScanningDirectory {

            @Test
            void Then_RespectsDeadlineAndReturnsEarlyOrEmpty() throws IOException {
                // Create many files in subdirectories so the deadline is checked
                for (int i = 0; i < 20; i++) {
                    var subDir = tempDir.resolve("pkg" + i);
                    Files.createDirectory(subDir);
                    Files.writeString(subDir.resolve("File" + i + ".java"),
                            "public class File" + i + " {}");
                }

                var reg = new LanguageRegistry();
                var bridge = new TreeSitterBridge(reg);
                var analyzers = List.<LanguageAnalyzer>of(new JavaTreeSitterAnalyzer());
                // maxRuntimeSeconds = 0 means the deadline is already in the past
                var scanner = new MultiLanguageSourceScanner(bridge, analyzers, reg, new AnalysisSafetyProperties(0, 1500, 30));

                var graph = scanner.scanDirectory(tempDir);

                // With a zero-second deadline the scan should terminate very early;
                // the preVisitDirectory check fires before descending into subdirs
                assertThat(graph.nodesOfType(KgNodeType.FILE).size())
                        .isLessThan(20);
            }
        }
    }

    @Nested
    class Given_EmptyDirectory {

        @TempDir
        Path tempDir;

        @Nested
        class When_ScanningDirectory {

            @Test
            void Then_ReturnsEmptyGraph() {
                var reg = new LanguageRegistry();
                var bridge = new TreeSitterBridge(reg);
                var analyzers = List.<LanguageAnalyzer>of(new GenericTreeSitterAnalyzer());
                var scanner = new MultiLanguageSourceScanner(bridge, analyzers, reg, new AnalysisSafetyProperties());

                var graph = scanner.scanDirectory(tempDir);

                assertThat(graph.nodeCount()).isEqualTo(0);
            }
        }
    }

    // ── CRITICAL 2: LanguageAnalyzer.analyze() template method tests ────

    @Nested
    class Given_AnalyzerWithMatchingLanguage {

        @Nested
        class When_AnalyzeCalledWithMatchingTree {

            @Test
            void Then_DispatchesToAnalyzeTree() {
                // arrange – java analyzer handles "java" trees
                var javaClass = new ParsedNode("class_declaration",
                        "public class Foo", 1, 5, 0, 1, List.of(
                        new ParsedNode("identifier", "Foo", 1, 1, 13, 16, List.of()),
                        new ParsedNode("class_body", "{}", 1, 5, 17, 1, List.of())
                ));
                var javaTree = new ParsedTree("Foo.java", "java", "source",
                        List.of(javaClass));
                var graph = new KnowledgeGraph();

                // act – call the public template method analyze()
                javaAnalyzer.analyze(javaTree, graph);

                // assert – analyzeTree was invoked, producing nodes
                assertThat(graph.nodesOfType(KgNodeType.FILE)).hasSize(1);
                assertThat(graph.nodesOfType(KgNodeType.CLASS)).hasSize(1);
            }
        }
    }

    @Nested
    class Given_AnalyzerWithNonMatchingLanguage {

        @Nested
        class When_AnalyzeCalledWithDifferentLanguageTree {

            @Test
            void Then_DoesNotAddAnyNodes() {
                // arrange – python tree passed to a java analyzer
                var pyFunc = new ParsedNode("function_definition",
                        "def main()", 1, 3, 0, 1, List.of(
                        new ParsedNode("identifier", "main", 1, 1, 4, 8, List.of())
                ));
                var pyTree = new ParsedTree("main.py", "python", "source",
                        List.of(pyFunc));
                var graph = new KnowledgeGraph();

                // act – java analyzer should reject a python tree
                javaAnalyzer.analyze(pyTree, graph);

                // assert – nothing was added
                assertThat(graph.nodeCount()).isEqualTo(0);
            }
        }
    }

    @Nested
    class Given_NullTreePassedToAnalyze {

        @Nested
        class When_AnalyzeCalled {

            @Test
            void Then_DoesNotThrowAndGraphIsEmpty() {
                var graph = new KnowledgeGraph();

                // act – null tree via the public template method
                javaAnalyzer.analyze(null, graph);

                // assert
                assertThat(graph.nodeCount()).isEqualTo(0);
            }
        }
    }

    @Nested
    class Given_PythonAnalyzerWithPythonTree {

        @Nested
        class When_AnalyzeCalled {

            @Test
            void Then_DispatchesToPythonAnalyzeTree() {
                var pyClass = new ParsedNode("class_definition",
                        "class Worker", 1, 4, 0, 1, List.of(
                        new ParsedNode("identifier", "Worker", 1, 1, 6, 12, List.of()),
                        new ParsedNode("block", "pass", 2, 4, 4, 8, List.of())
                ));
                var pyTree = new ParsedTree("worker.py", "python", "source",
                        List.of(pyClass));
                var graph = new KnowledgeGraph();

                // act – python analyzer, python tree: should match
                pythonAnalyzer.analyze(pyTree, graph);

                // assert – nodes created
                assertThat(graph.nodesOfType(KgNodeType.FILE)).hasSize(1);
                assertThat(graph.nodesOfType(KgNodeType.CLASS)).hasSize(1);
            }
        }
    }

    @Nested
    class Given_PythonAnalyzerWithJavaTree {

        @Nested
        class When_AnalyzeCalled {

            @Test
            void Then_ReturnsWithoutAddingNodes() {
                var javaClass = new ParsedNode("class_declaration",
                        "public class Bar", 1, 5, 0, 1, List.of(
                        new ParsedNode("identifier", "Bar", 1, 1, 13, 16, List.of()),
                        new ParsedNode("class_body", "{}", 1, 5, 17, 1, List.of())
                ));
                var javaTree = new ParsedTree("Bar.java", "java", "source",
                        List.of(javaClass));
                var graph = new KnowledgeGraph();

                // act – python analyzer rejects java tree
                pythonAnalyzer.analyze(javaTree, graph);

                // assert – nothing added
                assertThat(graph.nodeCount()).isEqualTo(0);
            }
        }
    }
}
