package com.codevision.codevisionbackend.analysis.multilang;

import com.codevision.codevisionbackend.graph.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Abstract base class for language-specific tree-sitter analyzers.
 * Each subclass handles one language's AST conventions and maps them
 * to the CodeVision knowledge graph model.
 *
 * <p>Uses the Template Method pattern: subclasses implement
 * {@link #languageName()} and {@link #analyzeTree(ParsedTree, KnowledgeGraph)}.
 * Common helper methods for creating nodes and edges are provided here.</p>
 */
public abstract class LanguageAnalyzer {

    private static final String ORIGIN_SOURCE = "SOURCE";

    /**
     * Returns the canonical language key this analyzer handles
     * (e.g. "java", "python", "typescript").
     *
     * @return language name matching {@link LanguageDefinition#name()}
     */
    public abstract String languageName();

    /**
     * Analyzes a parsed tree and populates the knowledge graph with
     * nodes and edges discovered in the source file.
     *
     * @param tree  the parsed tree-sitter output
     * @param graph the graph to populate
     */
    protected abstract void analyzeTree(ParsedTree tree, KnowledgeGraph graph);

    /**
     * Template method: delegates to {@link #analyzeTree} only when the tree's
     * language matches this analyzer's {@link #languageName()}.
     *
     * @param tree  the parsed AST
     * @param graph the graph to populate
     */
    public final void analyze(ParsedTree tree, KnowledgeGraph graph) {
        if (tree == null || !tree.languageName().equals(languageName())) {
            return;
        }
        analyzeTree(tree, graph);
    }

    // ── Helper methods ──────────────────────────────────────────────────

    /**
     * Creates a provenance record for this analyzer.
     */
    protected Provenance provenance(String sourceFile, int lineNumber) {
        return new Provenance(
                getClass().getSimpleName(),
                sourceFile,
                lineNumber,
                ConfidenceLevel.EXTRACTED
        );
    }

    /**
     * Creates a node ID from a file path and a qualified name.
     */
    protected String nodeId(String filePath, String qualifiedName) {
        return filePath + "#" + qualifiedName;
    }

    /**
     * Creates an edge ID from source and target node IDs plus the edge type.
     */
    protected String edgeId(String sourceNodeId, String targetNodeId, KgEdgeType edgeType) {
        return sourceNodeId + "-[" + edgeType.name() + "]->" + targetNodeId;
    }

    /**
     * Creates a KgNode with common defaults.
     */
    protected KgNode createNode(String id, KgNodeType type, String name,
                                String qualifiedName, String sourceFile,
                                int startLine, int endLine,
                                Map<String, Object> languageSpecific) {
        var metadata = new NodeMetadata(
                null, Set.of(), List.of(), List.of(),
                null, List.of(), List.of(), null,
                0, 0, Math.max(0, endLine - startLine + 1),
                null, sourceFile, startLine, endLine,
                languageSpecific
        );
        return new KgNode(id, type, name, qualifiedName, metadata,
                null, ORIGIN_SOURCE, provenance(sourceFile, startLine));
    }

    /**
     * Creates a KgEdge with common defaults.
     */
    protected KgEdge createEdge(String sourceNodeId, String targetNodeId,
                                KgEdgeType type, String label, String sourceFile,
                                int lineNumber) {
        return new KgEdge(
                edgeId(sourceNodeId, targetNodeId, type),
                type, sourceNodeId, targetNodeId, label,
                ConfidenceLevel.EXTRACTED,
                provenance(sourceFile, lineNumber),
                Map.of()
        );
    }

    /**
     * Finds the first direct child of a given type, or null if not found.
     */
    protected ParsedNode findChild(ParsedNode parent, String type) {
        for (var child : parent.children()) {
            if (type.equals(child.type())) {
                return child;
            }
        }
        return null;
    }

    /**
     * Finds all direct children of a given type.
     */
    protected List<ParsedNode> findChildren(ParsedNode parent, String type) {
        return parent.children().stream()
                .filter(c -> type.equals(c.type()))
                .toList();
    }

    /**
     * Extracts the name from a node by looking for a common "name" or "identifier" child.
     */
    protected String extractName(ParsedNode node) {
        var nameNode = findChild(node, "name");
        if (nameNode != null) {
            return nameNode.text().trim();
        }
        var identifier = findChild(node, "identifier");
        if (identifier != null) {
            return identifier.text().trim();
        }
        var typeIdentifier = findChild(node, "type_identifier");
        if (typeIdentifier != null) {
            return typeIdentifier.text().trim();
        }
        // Fallback: use the node text up to reasonable length
        var text = node.text().trim();
        var idx = text.indexOf('{');
        if (idx > 0) {
            text = text.substring(0, idx).trim();
        }
        idx = text.indexOf('(');
        if (idx > 0) {
            text = text.substring(0, idx).trim();
        }
        var parts = text.split("\\s+");
        return parts.length > 0 ? parts[parts.length - 1] : text;
    }
}
