package com.codevision.codevisionbackend.analysis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.codevision.codevisionbackend.analysis.multilang.MultiLanguageSourceScanner;
import com.codevision.codevisionbackend.graph.KgEdgeType;
import com.codevision.codevisionbackend.graph.KgNodeType;
import com.codevision.codevisionbackend.graph.KnowledgeGraph;
import com.codevision.codevisionbackend.graph.algorithm.GraphAlgorithm;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Integration test that points CodeVision's multi-language source scanner at its
 * own codebase. Validates that the scanner produces a non-trivial
 * {@link KnowledgeGraph} from real source files and that all registered
 * {@link GraphAlgorithm} beans execute without error on the result.
 */
@SpringBootTest
@ActiveProfiles("test")
class SelfAnalysisIT {

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",
                () -> "jdbc:h2:mem:codevision-self-analysis;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        registry.add("spring.datasource.driverClassName", () -> "org.h2.Driver");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.H2Dialect");
        registry.add("security.apiKey", () -> "test-key");
    }

    @Autowired
    MultiLanguageSourceScanner scanner;

    @Autowired(required = false)
    List<GraphAlgorithm<?>> algorithms;

    @Nested
    class Given_OwnSourceCode {

        KnowledgeGraph graph;
        Path sourcePath;

        @BeforeEach
        void setUp() {
            // Resolve source directory relative to the project root or module root
            sourcePath = Path.of("backend/api/src/main/java");
            if (!Files.isDirectory(sourcePath)) {
                sourcePath = Path.of("src/main/java");
            }
            Assumptions.assumeTrue(Files.isDirectory(sourcePath),
                    "Source directory must exist: " + sourcePath);
            graph = scanner.scanDirectory(sourcePath);
        }

        @Nested
        class When_ScanningDirectory {

            @Test
            void Then_GraphIsNotEmpty() {
                assertThat(graph.nodeCount()).isGreaterThan(0);
                assertThat(graph.edgeCount()).isGreaterThan(0);
            }

            @Test
            void Then_FindsKnownClasses() {
                var classNodes = graph.nodesOfType(KgNodeType.CLASS);
                assertThat(classNodes).isNotEmpty();

                boolean foundKnowledgeGraph = classNodes.stream()
                        .map(graph::getNode)
                        .filter(Objects::nonNull)
                        .anyMatch(n -> n.qualifiedName() != null
                                && n.qualifiedName().contains("KnowledgeGraph"));
                assertThat(foundKnowledgeGraph)
                        .as("should discover the KnowledgeGraph class from own source")
                        .isTrue();
            }

            @Test
            void Then_FindsPackages() {
                assertThat(graph.nodesOfType(KgNodeType.PACKAGE)).isNotEmpty();
            }

            @Test
            void Then_HasContainsEdges() {
                assertThat(graph.edgesOfType(KgEdgeType.CONTAINS)).isNotEmpty();
            }

            @Test
            void Then_FindsMethods() {
                assertThat(graph.nodesOfType(KgNodeType.METHOD)).isNotEmpty();
            }

            @Test
            void Then_FindsMultipleNodeTypes() {
                var nodeTypes = graph.getNodes().values().stream()
                        .map(n -> n.type())
                        .distinct()
                        .toList();
                assertThat(nodeTypes).hasSizeGreaterThanOrEqualTo(2);
            }
        }

        @Nested
        class When_RunningAlgorithms {

            @Test
            void Then_AllAlgorithmsExecuteWithoutException() {
                Assumptions.assumeTrue(algorithms != null && !algorithms.isEmpty(),
                        "At least one GraphAlgorithm bean must be registered");
                for (var algorithm : algorithms) {
                    assertDoesNotThrow(() -> algorithm.execute(graph),
                            "Algorithm " + algorithm.name() + " should not throw");
                }
            }
        }
    }
}
