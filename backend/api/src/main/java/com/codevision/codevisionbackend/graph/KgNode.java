package com.codevision.codevisionbackend.graph;

/**
 * Immutable value representing a single node in the CodeVision knowledge graph.
 *
 * @param id            globally unique node identifier (typically a content-addressable hash or UUID)
 * @param type          semantic type of this node
 * @param name          short / simple name (e.g. class simple name)
 * @param qualifiedName fully-qualified name usable as a cross-reference key
 * @param metadata      rich structural and metric metadata
 * @param artifactId    Maven/Gradle artifact coordinate when the node comes from an external dependency; {@code null} for project source
 * @param origin        how the node was discovered: {@code "SOURCE"}, {@code "BYTECODE"}, {@code "BOTH"}, or {@code "DEPENDENCY"}
 * @param provenance    scanner provenance for full traceability
 */
public record KgNode(
        String id,
        KgNodeType type,
        String name,
        String qualifiedName,
        NodeMetadata metadata,
        String artifactId,
        String origin,
        Provenance provenance
) {}
