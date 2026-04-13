package com.codevision.codevisionbackend.graph;

import com.codevision.codevisionbackend.analysis.GraphModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Fluent builder for constructing a {@link KnowledgeGraph} instance.
 *
 * <p>Usage example:</p>
 * <pre>{@code
 * KnowledgeGraph kg = new KnowledgeGraphBuilder()
 *         .withNode(myNode)
 *         .withEdge(myEdge)
 *         .build();
 * }</pre>
 *
 * <p>The builder also supports conversion from the legacy {@link GraphModel}
 * via {@link #fromGraphModel(GraphModel)}.</p>
 */
public class KnowledgeGraphBuilder {

    private final List<KgNode> pendingNodes = new ArrayList<>();
    private final List<KgEdge> pendingEdges = new ArrayList<>();

    /**
     * Queues a node for addition when {@link #build()} is called.
     *
     * @param node the node to add
     * @return this builder for chaining
     */
    public KnowledgeGraphBuilder withNode(KgNode node) {
        if (node != null) {
            pendingNodes.add(node);
        }
        return this;
    }

    /**
     * Queues an edge for addition when {@link #build()} is called.
     *
     * @param edge the edge to add
     * @return this builder for chaining
     */
    public KnowledgeGraphBuilder withEdge(KgEdge edge) {
        if (edge != null) {
            pendingEdges.add(edge);
        }
        return this;
    }

    /**
     * Materialises the {@link KnowledgeGraph} from all queued nodes and edges.
     *
     * @return a fully-populated knowledge graph
     */
    public KnowledgeGraph build() {
        KnowledgeGraph graph = new KnowledgeGraph();
        for (KgNode node : pendingNodes) {
            graph.addNode(node);
        }
        for (KgEdge edge : pendingEdges) {
            graph.addEdge(edge);
        }
        return graph;
    }

    /**
     * Converts a legacy {@link GraphModel} into a {@link KnowledgeGraph} by
     * delegating to {@link GraphModelAdapter#toKnowledgeGraph(GraphModel)}.
     *
     * @param graphModel the legacy model to convert
     * @return a new knowledge graph containing the converted data
     */
    public static KnowledgeGraph fromGraphModel(GraphModel graphModel) {
        return GraphModelAdapter.toKnowledgeGraph(graphModel);
    }
}
