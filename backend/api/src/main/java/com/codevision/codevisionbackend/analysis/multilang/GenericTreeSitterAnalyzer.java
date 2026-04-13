package com.codevision.codevisionbackend.analysis.multilang;

import com.codevision.codevisionbackend.graph.*;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Fallback analyzer that works for any language by inspecting common
 * tree-sitter AST node type names. Extracts classes, functions, interfaces,
 * and import relationships from generic patterns that appear across many
 * grammars.
 */
@Component
public class GenericTreeSitterAnalyzer extends LanguageAnalyzer {

    private static final Set<String> CLASS_TYPES = Set.of(
            "class_definition", "class_declaration"
    );

    private static final Set<String> METHOD_TYPES = Set.of(
            "function_definition", "function_declaration", "method_definition", "method_declaration"
    );

    private static final Set<String> INTERFACE_TYPES = Set.of(
            "interface_declaration", "interface_definition"
    );

    private static final Set<String> IMPORT_TYPES = Set.of(
            "import_statement", "import_declaration"
    );

    @Override
    public String languageName() {
        return "generic";
    }

    /**
     * Directly analyzes a tree regardless of language matching.
     * Used as a fallback when no language-specific analyzer is available.
     *
     * @param tree  the parsed tree
     * @param graph the graph to populate
     */
    public void analyzeGeneric(ParsedTree tree, KnowledgeGraph graph) {
        if (tree == null) {
            return;
        }
        analyzeTree(tree, graph);
    }

    @Override
    protected void analyzeTree(ParsedTree tree, KnowledgeGraph graph) {
        var sourceFile = tree.sourceFile();

        // Create the file node as a PACKAGE container
        var fileNodeId = nodeId(sourceFile, sourceFile);
        var fileNode = createNode(fileNodeId, KgNodeType.FILE, sourceFile,
                sourceFile, sourceFile, 1, 1, Map.of("language", tree.languageName()));
        graph.addNode(fileNode);

        // Walk root nodes with cycle detection
        var visited = new HashSet<String>();
        for (var rootNode : tree.rootNodes()) {
            walkNode(rootNode, fileNodeId, sourceFile, graph, visited);
        }
    }

    private void walkNode(ParsedNode node, String parentNodeId,
                          String sourceFile, KnowledgeGraph graph,
                          Set<String> visited) {
        var visitKey = sourceFile + ":" + node.startLine() + ":" + node.startColumn() + ":" + node.type();
        if (!visited.add(visitKey)) {
            return;
        }

        if (CLASS_TYPES.contains(node.type())) {
            var name = extractName(node);
            var classNodeId = nodeId(sourceFile, name);
            var classNode = createNode(classNodeId, KgNodeType.CLASS, name,
                    name, sourceFile, node.startLine(), node.endLine(), Map.of());
            graph.addNode(classNode);
            graph.addEdge(createEdge(parentNodeId, classNodeId, KgEdgeType.CONTAINS,
                    "contains", sourceFile, node.startLine()));

            // Walk children under this class
            for (var child : node.children()) {
                walkNode(child, classNodeId, sourceFile, graph, visited);
            }
            return;
        }

        if (METHOD_TYPES.contains(node.type())) {
            var name = extractName(node);
            var methodNodeId = nodeId(sourceFile, parentNodeId + "." + name);
            var methodNode = createNode(methodNodeId, KgNodeType.METHOD, name,
                    name, sourceFile, node.startLine(), node.endLine(), Map.of());
            graph.addNode(methodNode);
            graph.addEdge(createEdge(parentNodeId, methodNodeId, KgEdgeType.CONTAINS,
                    "contains", sourceFile, node.startLine()));
            return;
        }

        if (INTERFACE_TYPES.contains(node.type())) {
            var name = extractName(node);
            var ifaceNodeId = nodeId(sourceFile, name);
            var ifaceNode = createNode(ifaceNodeId, KgNodeType.INTERFACE, name,
                    name, sourceFile, node.startLine(), node.endLine(), Map.of());
            graph.addNode(ifaceNode);
            graph.addEdge(createEdge(parentNodeId, ifaceNodeId, KgEdgeType.CONTAINS,
                    "contains", sourceFile, node.startLine()));
            return;
        }

        if (IMPORT_TYPES.contains(node.type())) {
            var importText = node.text().trim();
            var importNodeId = nodeId(sourceFile, "import:" + importText);
            graph.addEdge(createEdge(parentNodeId, importNodeId, KgEdgeType.IMPORTS,
                    importText, sourceFile, node.startLine()));
            return;
        }

        // Recurse into children for unrecognized node types
        for (var child : node.children()) {
            walkNode(child, parentNodeId, sourceFile, graph, visited);
        }
    }
}
