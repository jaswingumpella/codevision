package com.codevision.codevisionbackend.graph.persistence;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

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
import org.springframework.jdbc.core.ParameterizedPreparedStatementSetter;

import com.codevision.codevisionbackend.graph.ConfidenceLevel;
import com.codevision.codevisionbackend.graph.KgEdge;
import com.codevision.codevisionbackend.graph.KgEdgeType;
import com.codevision.codevisionbackend.graph.KgNode;
import com.codevision.codevisionbackend.graph.KgNodeType;
import com.codevision.codevisionbackend.graph.KnowledgeGraph;
import com.codevision.codevisionbackend.graph.NodeMetadata;
import com.codevision.codevisionbackend.graph.Provenance;
import com.fasterxml.jackson.databind.ObjectMapper;

@DisplayName("KnowledgeGraphPersistService")
@ExtendWith(MockitoExtension.class)
class KnowledgeGraphPersistServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private KnowledgeGraphPersistService persistService;

    @BeforeEach
    void setUp() {
        // Use a real ObjectMapper so toJson() works without mock setup
        persistService = new KnowledgeGraphPersistService(jdbcTemplate, new ObjectMapper(), 500);
    }

    private static KgNode classNode(String id, String name) {
        return new KgNode(id, KgNodeType.CLASS, name, "com.test." + name,
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
    @DisplayName("Given graph with nodes and edges")
    class Given_GraphWithNodes {

        @Nested
        @DisplayName("When persisting")
        class When_Persisting {

            @Test
            @DisplayName("Then batchUpdate called for nodes and edges")
            @SuppressWarnings("unchecked")
            void Then_BatchUpdateCalledForNodesAndEdges() {
                var graph = new KnowledgeGraph();
                graph.addNode(classNode("n1", "ClassA"));
                graph.addEdge(edge("e1", KgEdgeType.CALLS, "n1", "n1"));

                persistService.persist(1L, graph);

                // Verify batchUpdate is called at least once (for nodes and/or edges)
                verify(jdbcTemplate, atLeastOnce()).batchUpdate(
                        anyString(), any(java.util.Collection.class), anyInt(),
                        any(ParameterizedPreparedStatementSetter.class));
            }
        }
    }

    @Nested
    @DisplayName("Given existing data")
    class Given_ExistingData {

        @Nested
        @DisplayName("When persisting")
        class When_Persisting {

            @Test
            @DisplayName("Then delete called before insert")
            @SuppressWarnings("unchecked")
            void Then_DeleteCalledBeforeInsert() {
                var graph = new KnowledgeGraph();
                graph.addNode(classNode("n1", "ClassA"));

                persistService.persist(1L, graph);

                // Delete is called for both edges and nodes tables
                var inOrder = org.mockito.Mockito.inOrder(jdbcTemplate);
                inOrder.verify(jdbcTemplate, atLeastOnce()).update(anyString(), eq(1L));
                inOrder.verify(jdbcTemplate, atLeastOnce()).batchUpdate(
                        anyString(), any(java.util.Collection.class), anyInt(),
                        any(ParameterizedPreparedStatementSetter.class));
            }
        }
    }

    @Nested
    @DisplayName("Given empty graph")
    class Given_EmptyGraph {

        @Nested
        @DisplayName("When persisting")
        class When_Persisting {

            @Test
            @DisplayName("Then no batch inserts called")
            @SuppressWarnings("unchecked")
            void Then_NoBatchInsertsCalled() {
                var graph = new KnowledgeGraph();

                persistService.persist(1L, graph);

                verify(jdbcTemplate, never()).batchUpdate(
                        anyString(), any(java.util.Collection.class), anyInt(),
                        any(ParameterizedPreparedStatementSetter.class));
            }
        }
    }

    @Nested
    @DisplayName("Given deleteByProjectId")
    class Given_DeleteByProjectId {

        @Nested
        @DisplayName("When called")
        class When_Called {

            @Test
            @DisplayName("Then both tables are deleted")
            void Then_BothTablesAreDeleted() {
                persistService.deleteByProjectId(1L);

                // Two update calls: one for edges, one for nodes
                verify(jdbcTemplate).update(
                        org.mockito.ArgumentMatchers.contains("kg_edge"), eq(1L));
                verify(jdbcTemplate).update(
                        org.mockito.ArgumentMatchers.contains("kg_node"), eq(1L));
            }
        }
    }
}
