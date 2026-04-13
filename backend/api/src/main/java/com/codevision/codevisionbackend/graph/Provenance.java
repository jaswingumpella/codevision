package com.codevision.codevisionbackend.graph;

/**
 * Immutable provenance record that tracks where a graph element was discovered.
 * Every {@link KgNode} and {@link KgEdge} carries provenance so that results
 * are fully traceable back to a scanner, source file, and line.
 *
 * @param scannerName  the name of the scanner or analysis pass that produced the element
 * @param sourceFile   path to the source or class file that was analysed
 * @param lineNumber   one-based line number within {@code sourceFile} (0 when unavailable)
 * @param confidence   confidence level assigned by the producing scanner
 */
public record Provenance(
        String scannerName,
        String sourceFile,
        int lineNumber,
        ConfidenceLevel confidence
) {}
