package com.codevision.codevisionbackend.usecase;

import com.codevision.codevisionbackend.graph.KgEdgeType;
import com.codevision.codevisionbackend.graph.KgNodeType;
import com.codevision.codevisionbackend.graph.KnowledgeGraph;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Use case: "Show me my DB schema as a graph with entity relationships."
 * Extracts database entities and their relationships from the knowledge graph.
 */
@Service
public class DbSchemaIntelligenceService {

    public record SchemaReport(List<EntityInfo> entities, List<RelationshipInfo> relationships) {}
    public record EntityInfo(String id, String name, int columnCount) {}
    public record RelationshipInfo(String sourceEntity, String targetEntity, String type) {}

    public SchemaReport analyze(KnowledgeGraph graph) {
        var entityIds = graph.nodesOfType(KgNodeType.DATABASE_ENTITY);
        if (entityIds.isEmpty()) {
            return new SchemaReport(List.of(), List.of());
        }

        var entities = new ArrayList<EntityInfo>();
        for (var id : entityIds) {
            var node = graph.getNode(id);
            int columns = (int) graph.getNeighbors(id).stream()
                    .filter(e -> e.type() == KgEdgeType.CONTAINS)
                    .count();
            entities.add(new EntityInfo(id, node != null ? node.name() : id, columns));
        }

        var relationships = new ArrayList<RelationshipInfo>();
        for (var edge : graph.edgesOfType(KgEdgeType.RELATES_TO)) {
            relationships.add(new RelationshipInfo(
                    edge.sourceNodeId(), edge.targetNodeId(), "RELATES_TO"));
        }
        for (var edge : graph.edgesOfType(KgEdgeType.JOINS)) {
            relationships.add(new RelationshipInfo(
                    edge.sourceNodeId(), edge.targetNodeId(), "JOINS"));
        }

        return new SchemaReport(entities, relationships);
    }
}
