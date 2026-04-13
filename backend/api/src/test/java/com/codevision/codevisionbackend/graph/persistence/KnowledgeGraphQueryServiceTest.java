package com.codevision.codevisionbackend.graph.persistence;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import com.codevision.codevisionbackend.graph.ConfidenceLevel;
import com.codevision.codevisionbackend.graph.KgEdge;
import com.codevision.codevisionbackend.graph.KgEdgeType;
import com.codevision.codevisionbackend.graph.KgNode;
import com.codevision.codevisionbackend.graph.KgNodeType;
import com.codevision.codevisionbackend.graph.NodeMetadata;
import com.codevision.codevisionbackend.graph.Provenance;
import com.fasterxml.jackson.databind.ObjectMapper;

@DisplayName("KnowledgeGraphQueryService")
@ExtendWith(MockitoExtension.class)
class KnowledgeGraphQueryServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private KnowledgeGraphQueryService queryService;

    @BeforeEach
    void setUp() {
        queryService = new KnowledgeGraphQueryService(jdbcTemplate, new ObjectMapper());
    }

    private static KgNode classNode(String id, String name) {
        return new KgNode(id, KgNodeType.CLASS, name, "com.test." + name,
                new NodeMetadata("public", Set.of(), emptyList(), emptyList(), "void",
                        emptyList(), emptyList(), null, 0, 0, 0, null, null, 0, 0, null),
                null, "SOURCE",
                new Provenance("test", "Test.java", 1, ConfidenceLevel.EXTRACTED));
    }

    private static KgNode methodNode(String id, String name) {
        return new KgNode(id, KgNodeType.METHOD, name, "com.test." + name,
                new NodeMetadata("public", Set.of(), emptyList(), emptyList(), "void",
                        emptyList(), emptyList(), null, 0, 0, 0, null, null, 0, 0, null),
                null, "SOURCE",
                new Provenance("test", "Test.java", 1, ConfidenceLevel.EXTRACTED));
    }

    private static KgEdge edge(String id, KgEdgeType type, String source, String target) {
        return new KgEdge(id, type, source, target, type.name(), ConfidenceLevel.EXTRACTED,
                new Provenance("test", "Test.java", 1, ConfidenceLevel.EXTRACTED), Map.of());
    }

    @Nested
    @DisplayName("Given persisted graph")
    class Given_PersistedGraph {

        @Nested
        @DisplayName("When loading full graph")
        class When_Loading {

            @Test
            @DisplayName("Then nodes and edges reconstructed")
            @SuppressWarnings("unchecked")
            void Then_NodesAndEdgesReconstructed() {
                var node1 = classNode("n1", "ClassA");
                var node2 = methodNode("n2", "methodB");
                var edgeVal = edge("e1", KgEdgeType.CALLS, "n1", "n2");

                // loadGraph calls query twice: once for nodes, once for edges
                when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(1L)))
                        .thenReturn(List.of(node1, node2))
                        .thenReturn(List.of(edgeVal));

                var graph = queryService.loadGraph(1L);

                assertThat(graph).isNotNull();
                assertThat(graph.nodeCount()).isEqualTo(2);
                assertThat(graph.edgeCount()).isEqualTo(1);
                assertThat(graph.getNode("n1")).isNotNull();
                assertThat(graph.getNode("n1").name()).isEqualTo("ClassA");
            }
        }
    }

    @Nested
    @DisplayName("Given empty project")
    class Given_EmptyProject {

        @Nested
        @DisplayName("When loading")
        class When_Loading {

            @Test
            @DisplayName("Then empty graph returned")
            @SuppressWarnings("unchecked")
            void Then_EmptyGraphReturned() {
                when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(2L)))
                        .thenReturn(emptyList());

                var graph = queryService.loadGraph(2L);

                assertThat(graph).isNotNull();
                assertThat(graph.nodeCount()).isZero();
                assertThat(graph.edgeCount()).isZero();
            }
        }
    }

    @Nested
    @DisplayName("Given transitive dependency query")
    class Given_TransitiveDependencyQuery {

        @Nested
        @DisplayName("When finding dependencies")
        class When_FindingDependencies {

            @Test
            @DisplayName("Then returns set of dependent node ids")
            void Then_ReturnsSetOfDependentNodeIds() {
                when(jdbcTemplate.queryForList(anyString(), eq(String.class), eq(1L), eq("root"), eq(1L)))
                        .thenReturn(List.of("dep1", "dep2", "dep3"));

                var deps = queryService.findTransitiveDependencies(1L, "root");

                assertThat(deps).containsExactlyInAnyOrder("dep1", "dep2", "dep3");
            }
        }
    }

    @Nested
    @DisplayName("Given subgraph request")
    class Given_SubgraphRequest {

        @Nested
        @DisplayName("When loading subgraph with empty node set")
        class When_LoadingEmptySubgraph {

            @Test
            @DisplayName("Then returns empty graph without DB calls")
            void Then_ReturnsEmptyGraphWithoutDBCalls() {
                var graph = queryService.loadSubgraph(1L, Set.of());

                assertThat(graph).isNotNull();
                assertThat(graph.nodeCount()).isZero();
                assertThat(graph.edgeCount()).isZero();
            }
        }

        @Nested
        @DisplayName("When loading subgraph with valid nodes")
        class When_LoadingWithValidNodes {

            @Test
            @DisplayName("Then nodes and filtered edges returned")
            @SuppressWarnings("unchecked")
            void Then_NodesAndFilteredEdgesReturned() {
                var node1 = classNode("n1", "ClassA");
                var node2 = classNode("n2", "ClassB");
                var edgeInside = edge("e1", KgEdgeType.EXTENDS, "n2", "n1");
                var edgeOutside = edge("e2", KgEdgeType.CALLS, "n1", "outside");

                // Match node query (contains "kg_node") vs edge query (contains "kg_edge")
                when(jdbcTemplate.query(
                        org.mockito.ArgumentMatchers.contains("kg_node"),
                        any(RowMapper.class), any(), any()))
                        .thenReturn(List.of(node1, node2));
                when(jdbcTemplate.query(
                        org.mockito.ArgumentMatchers.contains("kg_edge"),
                        any(RowMapper.class), any(), any(), any()))
                        .thenReturn(List.of(edgeInside, edgeOutside));

                var graph = queryService.loadSubgraph(1L, Set.of("n1", "n2"));

                assertThat(graph).isNotNull();
                assertThat(graph.nodeCount()).isEqualTo(2);
                // Only the inside edge should be included (filtering logic)
                assertThat(graph.edgeCount()).isEqualTo(1);
                assertThat(graph.getEdges().get(0).id()).isEqualTo("e1");
            }
        }
    }

    @Nested
    @DisplayName("Given shortest path query")
    class Given_ShortestPathQuery {

        @Nested
        @DisplayName("When no path exists")
        class When_NoPathExists {

            @Test
            @DisplayName("Then returns empty list")
            void Then_ReturnsEmptyList() {
                when(jdbcTemplate.queryForList(anyString(), eq(1L), eq("a"), eq(1L), eq("z")))
                        .thenReturn(emptyList());

                var path = queryService.findShortestPath(1L, "a", "z");

                assertThat(path).isEmpty();
            }
        }
    }
}
