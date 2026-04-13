package com.codevision.codevisionbackend.graph.persistence;

import com.codevision.codevisionbackend.graph.ConfidenceLevel;
import com.codevision.codevisionbackend.graph.KgEdge;
import com.codevision.codevisionbackend.graph.KgEdgeType;
import com.codevision.codevisionbackend.graph.KgNode;
import com.codevision.codevisionbackend.graph.KgNodeType;
import com.codevision.codevisionbackend.graph.KnowledgeGraph;
import com.codevision.codevisionbackend.graph.NodeMetadata;
import com.codevision.codevisionbackend.graph.Provenance;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Loads {@link KnowledgeGraph} instances from PostgreSQL ({@code kg_node},
 * {@code kg_edge} tables). Read-only service that deserialises JSONB columns
 * back to Java records using Jackson.
 */
@Service
@Transactional(readOnly = true)
public class KnowledgeGraphQueryService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeGraphQueryService.class);

    private static final String SELECT_NODES_SQL =
            "SELECT id, project_id, type, name, qualified_name, metadata, provenance, confidence, artifact_id, origin "
                    + "FROM kg_node WHERE project_id = ?";

    private static final String SELECT_EDGES_SQL =
            "SELECT id, project_id, source_node_id, target_node_id, type, label, confidence, provenance, properties "
                    + "FROM kg_edge WHERE project_id = ?";

    private static final String SELECT_NODES_BY_IDS_SQL =
            "SELECT id, project_id, type, name, qualified_name, metadata, provenance, confidence, artifact_id, origin "
                    + "FROM kg_node WHERE project_id = ? AND id = ANY(?)";

    private static final String SELECT_EDGES_FOR_NODES_SQL =
            "SELECT id, project_id, source_node_id, target_node_id, type, label, confidence, provenance, properties "
                    + "FROM kg_edge WHERE project_id = ? AND (source_node_id = ANY(?) OR target_node_id = ANY(?))";

    private static final String TRANSITIVE_DEPENDENCIES_SQL =
            "WITH RECURSIVE transitive AS ("
                    + "  SELECT target_node_id AS node_id FROM kg_edge "
                    + "  WHERE project_id = ? AND source_node_id = ? AND type = 'DEPENDS_ON' "
                    + "  UNION "
                    + "  SELECT e.target_node_id FROM kg_edge e "
                    + "  INNER JOIN transitive t ON e.source_node_id = t.node_id "
                    + "  WHERE e.project_id = ? AND e.type = 'DEPENDS_ON'"
                    + ") SELECT DISTINCT node_id FROM transitive";

    private static final String SHORTEST_PATH_SQL =
            "WITH RECURSIVE path AS ("
                    + "  SELECT source_node_id, target_node_id, ARRAY[source_node_id, target_node_id] AS trail "
                    + "  FROM kg_edge WHERE project_id = ? AND source_node_id = ? "
                    + "  UNION ALL "
                    + "  SELECT p.source_node_id, e.target_node_id, p.trail || e.target_node_id "
                    + "  FROM path p "
                    + "  INNER JOIN kg_edge e ON e.source_node_id = p.target_node_id "
                    + "  WHERE e.project_id = ? AND NOT (e.target_node_id = ANY(p.trail))"
                    + ") SELECT trail FROM path WHERE target_node_id = ? ORDER BY array_length(trail, 1) LIMIT 1";

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public KnowledgeGraphQueryService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate);
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    /**
     * Loads the full knowledge graph for a project.
     *
     * @param projectId the project identifier
     * @return a fully populated {@link KnowledgeGraph}
     */
    public KnowledgeGraph loadGraph(Long projectId) {
        Objects.requireNonNull(projectId, "projectId must not be null");

        KnowledgeGraph graph = new KnowledgeGraph();

        List<KgNode> nodes = jdbcTemplate.query(SELECT_NODES_SQL, nodeRowMapper(), projectId);
        for (KgNode node : nodes) {
            graph.addNode(node);
        }

        List<KgEdge> edges = jdbcTemplate.query(SELECT_EDGES_SQL, edgeRowMapper(), projectId);
        for (KgEdge edge : edges) {
            graph.addEdge(edge);
        }

        log.debug("Loaded knowledge graph for project {}: {} nodes, {} edges",
                projectId, graph.nodeCount(), graph.edgeCount());

        return graph;
    }

    /**
     * Loads a subgraph containing only the specified nodes and their connecting edges.
     *
     * @param projectId the project identifier
     * @param nodeIds   the set of node ids to include
     * @return a {@link KnowledgeGraph} containing only the requested nodes and their edges
     */
    public KnowledgeGraph loadSubgraph(Long projectId, Set<String> nodeIds) {
        Objects.requireNonNull(projectId, "projectId must not be null");
        Objects.requireNonNull(nodeIds, "nodeIds must not be null");

        if (nodeIds.isEmpty()) {
            return new KnowledgeGraph();
        }

        KnowledgeGraph graph = new KnowledgeGraph();
        String[] idArray = nodeIds.toArray(String[]::new);

        List<KgNode> nodes = jdbcTemplate.query(SELECT_NODES_BY_IDS_SQL, nodeRowMapper(), projectId, idArray);
        for (KgNode node : nodes) {
            graph.addNode(node);
        }

        List<KgEdge> edges = jdbcTemplate.query(SELECT_EDGES_FOR_NODES_SQL, edgeRowMapper(),
                projectId, idArray, idArray);
        for (KgEdge edge : edges) {
            // Only include edges where both endpoints are in the subgraph
            if (nodeIds.contains(edge.sourceNodeId()) && nodeIds.contains(edge.targetNodeId())) {
                graph.addEdge(edge);
            }
        }

        return graph;
    }

    /**
     * Finds all transitive dependencies of a node using a recursive CTE.
     *
     * @param projectId the project identifier
     * @param nodeId    the starting node id
     * @return set of transitively-dependent node ids
     */
    public Set<String> findTransitiveDependencies(Long projectId, String nodeId) {
        Objects.requireNonNull(projectId, "projectId must not be null");
        Objects.requireNonNull(nodeId, "nodeId must not be null");

        List<String> results = jdbcTemplate.queryForList(
                TRANSITIVE_DEPENDENCIES_SQL,
                String.class,
                projectId, nodeId, projectId);

        return Collections.unmodifiableSet(new HashSet<>(results));
    }

    /**
     * Finds the shortest path between two nodes using a recursive CTE.
     *
     * @param projectId the project identifier
     * @param fromId    the source node id
     * @param toId      the target node id
     * @return ordered list of node ids forming the shortest path, or empty list if unreachable
     */
    public List<String> findShortestPath(Long projectId, String fromId, String toId) {
        Objects.requireNonNull(projectId, "projectId must not be null");
        Objects.requireNonNull(fromId, "fromId must not be null");
        Objects.requireNonNull(toId, "toId must not be null");

        List<Map<String, Object>> results = jdbcTemplate.queryForList(
                SHORTEST_PATH_SQL,
                projectId, fromId, projectId, toId);

        if (results.isEmpty()) {
            return List.of();
        }

        Object trailObj = results.get(0).get("trail");
        if (trailObj instanceof String[] trail) {
            return List.of(trail);
        }
        // PostgreSQL JDBC may return java.sql.Array
        if (trailObj instanceof java.sql.Array sqlArray) {
            try {
                Object[] arr = (Object[]) sqlArray.getArray();
                List<String> path = new ArrayList<>(arr.length);
                for (Object o : arr) {
                    path.add(o.toString());
                }
                return Collections.unmodifiableList(path);
            } catch (SQLException e) {
                log.warn("Failed to extract shortest path array: {}", e.getMessage());
                return List.of();
            }
        }

        return List.of();
    }

    // ── Row mappers ─────────────────────────────────────────────────────────

    private RowMapper<KgNode> nodeRowMapper() {
        return (rs, rowNum) -> {
            String typeStr = rs.getString("type");
            KgNodeType type = typeStr != null ? KgNodeType.valueOf(typeStr) : null;

            NodeMetadata metadata = fromJson(rs.getString("metadata"), NodeMetadata.class);
            Provenance provenance = fromJson(rs.getString("provenance"), Provenance.class);

            return new KgNode(
                    rs.getString("id"),
                    type,
                    rs.getString("name"),
                    rs.getString("qualified_name"),
                    metadata,
                    rs.getString("artifact_id"),
                    rs.getString("origin"),
                    provenance
            );
        };
    }

    private RowMapper<KgEdge> edgeRowMapper() {
        return (rs, rowNum) -> {
            String typeStr = rs.getString("type");
            KgEdgeType type = typeStr != null ? KgEdgeType.valueOf(typeStr) : null;

            String confidenceStr = rs.getString("confidence");
            ConfidenceLevel confidence = confidenceStr != null ? ConfidenceLevel.valueOf(confidenceStr) : null;

            Provenance provenance = fromJson(rs.getString("provenance"), Provenance.class);
            Map<String, Object> properties = fromJson(rs.getString("properties"),
                    new TypeReference<Map<String, Object>>() {});

            return new KgEdge(
                    rs.getString("id"),
                    type,
                    rs.getString("source_node_id"),
                    rs.getString("target_node_id"),
                    rs.getString("label"),
                    confidence,
                    provenance,
                    properties
            );
        };
    }

    private <T> T fromJson(String json, Class<T> type) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialise JSON to {}: {}", type.getSimpleName(), e.getMessage());
            return null;
        }
    }

    private <T> T fromJson(String json, TypeReference<T> typeRef) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, typeRef);
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialise JSON: {}", e.getMessage());
            return null;
        }
    }
}
