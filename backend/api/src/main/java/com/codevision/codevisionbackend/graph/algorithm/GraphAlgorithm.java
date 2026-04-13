package com.codevision.codevisionbackend.graph.algorithm;

import com.codevision.codevisionbackend.graph.KnowledgeGraph;

/**
 * Command interface for pluggable graph algorithms.
 * Each algorithm takes a KnowledgeGraph and produces a typed result.
 *
 * @param <R> the result type produced by this algorithm
 */
public interface GraphAlgorithm<R> {

    /**
     * Returns a human-readable name for this algorithm.
     */
    String name();

    /**
     * Executes the algorithm on the given graph.
     *
     * @param graph the knowledge graph to analyze
     * @return the computed result
     */
    R execute(KnowledgeGraph graph);
}
