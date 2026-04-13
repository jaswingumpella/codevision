package com.codevision.codevisionbackend.graph;

/**
 * Describes how confident the analysis pipeline is about a particular graph
 * element. Every node and edge carries a confidence score so that downstream
 * consumers can filter or weight results accordingly.
 */
public enum ConfidenceLevel {

    /** Directly extracted from source or bytecode with no ambiguity. */
    EXTRACTED(1.0),

    /** Resolved through cross-referencing (e.g. symbol resolution). */
    RESOLVED(0.95),

    /** Inferred via heuristics such as naming conventions or patterns. */
    INFERRED(0.7),

    /** Multiple candidates exist; the link is speculative. */
    AMBIGUOUS(0.3);

    private final double score;

    ConfidenceLevel(double score) {
        this.score = score;
    }

    /**
     * Returns the numeric confidence score in the range {@code [0.0, 1.0]}.
     *
     * @return confidence score
     */
    public double score() {
        return score;
    }
}
