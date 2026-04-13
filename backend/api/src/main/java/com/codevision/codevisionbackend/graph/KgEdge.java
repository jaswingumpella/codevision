package com.codevision.codevisionbackend.graph;

import java.util.Map;

/**
 * Immutable value representing a single directed edge in the CodeVision
 * knowledge graph.
 *
 * @param id            globally unique edge identifier
 * @param type          semantic type of this relationship
 * @param sourceNodeId  identifier of the origin {@link KgNode}
 * @param targetNodeId  identifier of the destination {@link KgNode}
 * @param label         human-readable label (e.g. method signature, dependency scope)
 * @param confidence    how confident the analysis is about this edge
 * @param provenance    scanner provenance for full traceability
 * @param properties    arbitrary key-value pairs for edge-specific data (e.g. call site line, weight)
 */
public record KgEdge(
        String id,
        KgEdgeType type,
        String sourceNodeId,
        String targetNodeId,
        String label,
        ConfidenceLevel confidence,
        Provenance provenance,
        Map<String, Object> properties
) {}
