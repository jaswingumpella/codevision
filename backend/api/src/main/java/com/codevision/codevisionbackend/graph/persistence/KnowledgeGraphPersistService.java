package com.codevision.codevisionbackend.graph.persistence;

import com.codevision.codevisionbackend.graph.ConfidenceLevel;
import com.codevision.codevisionbackend.graph.KgEdge;
import com.codevision.codevisionbackend.graph.KgNode;
import com.codevision.codevisionbackend.graph.KnowledgeGraph;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Persists a {@link KnowledgeGraph} to PostgreSQL tables ({@code kg_node},
 * {@code kg_edge}) using batch inserts via {@link JdbcTemplate}. Node metadata
 * and edge properties are serialised as JSONB using Jackson.
 */
@Service
@Transactional
public class KnowledgeGraphPersistService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeGraphPersistService.class);

    private static final String INSERT_NODE_SQL =
            "INSERT INTO kg_node (id, project_id, type, name, qualified_name, metadata, provenance, confidence, artifact_id, origin) "
                    + "VALUES (?, ?, ?, ?, ?, ?::jsonb, ?::jsonb, ?, ?, ?)";

    private static final String INSERT_EDGE_SQL =
            "INSERT INTO kg_edge (id, project_id, source_node_id, target_node_id, type, label, confidence, provenance, properties) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?::jsonb)";

    private static final String DELETE_NODES_SQL = "DELETE FROM kg_node WHERE project_id = ?";
    private static final String DELETE_EDGES_SQL = "DELETE FROM kg_edge WHERE project_id = ?";

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final int batchSize;

    public KnowledgeGraphPersistService(
            JdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper,
            @Value("${graph.persistence.batchSize:500}") int batchSize) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate);
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.batchSize = batchSize;
    }

    /**
     * Persists the entire knowledge graph for a project. Deletes existing data
     * for the project first (clean-slate approach), then batch-inserts all
     * nodes and edges.
     *
     * @param projectId the project identifier
     * @param graph     the knowledge graph to persist
     */
    public void persist(Long projectId, KnowledgeGraph graph) {
        Objects.requireNonNull(projectId, "projectId must not be null");
        Objects.requireNonNull(graph, "graph must not be null");

        deleteByProjectId(projectId);
        batchInsertNodes(projectId, graph);
        batchInsertEdges(projectId, graph);

        log.info("Persisted knowledge graph for project {}: {} nodes, {} edges",
                projectId, graph.nodeCount(), graph.edgeCount());
    }

    /**
     * Deletes all knowledge graph data for the given project.
     *
     * @param projectId the project identifier
     */
    public void deleteByProjectId(Long projectId) {
        Objects.requireNonNull(projectId, "projectId must not be null");

        int edgesDeleted = jdbcTemplate.update(DELETE_EDGES_SQL, projectId);
        int nodesDeleted = jdbcTemplate.update(DELETE_NODES_SQL, projectId);

        log.debug("Deleted {} nodes and {} edges for project {}", nodesDeleted, edgesDeleted, projectId);
    }

    // ── Internal helpers ────────────────────────────────────────────────────

    private void batchInsertNodes(Long projectId, KnowledgeGraph graph) {
        List<KgNode> nodes = new ArrayList<>(graph.getNodes().values());

        for (int i = 0; i < nodes.size(); i += batchSize) {
            List<KgNode> batch = nodes.subList(i, Math.min(i + batchSize, nodes.size()));

            jdbcTemplate.batchUpdate(INSERT_NODE_SQL, batch, batch.size(),
                    (ps, node) -> {
                        ps.setString(1, node.id());
                        ps.setLong(2, projectId);
                        ps.setString(3, node.type() != null ? node.type().name() : null);
                        ps.setString(4, node.name());
                        ps.setString(5, node.qualifiedName());
                        ps.setString(6, toJson(node.metadata()));
                        ps.setString(7, toJson(node.provenance()));
                        ps.setString(8, confidenceFromProvenance(node));
                        ps.setString(9, node.artifactId());
                        ps.setString(10, node.origin());
                    });
        }
    }

    private void batchInsertEdges(Long projectId, KnowledgeGraph graph) {
        List<KgEdge> edges = graph.getEdges();

        for (int i = 0; i < edges.size(); i += batchSize) {
            List<KgEdge> batch = edges.subList(i, Math.min(i + batchSize, edges.size()));

            jdbcTemplate.batchUpdate(INSERT_EDGE_SQL, batch, batch.size(),
                    (ps, edge) -> {
                        ps.setString(1, edge.id());
                        ps.setLong(2, projectId);
                        ps.setString(3, edge.sourceNodeId());
                        ps.setString(4, edge.targetNodeId());
                        ps.setString(5, edge.type() != null ? edge.type().name() : null);
                        ps.setString(6, edge.label());
                        ps.setString(7, edge.confidence() != null ? edge.confidence().name() : null);
                        ps.setString(8, toJson(edge.provenance()));
                        ps.setString(9, toJson(edge.properties()));
                    });
        }
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialise value to JSON: {}", e.getMessage());
            return null;
        }
    }

    private String confidenceFromProvenance(KgNode node) {
        if (node.provenance() != null && node.provenance().confidence() != null) {
            return node.provenance().confidence().name();
        }
        return null;
    }
}
