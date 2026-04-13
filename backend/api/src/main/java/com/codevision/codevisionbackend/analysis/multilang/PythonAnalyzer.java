package com.codevision.codevisionbackend.analysis.multilang;

import com.codevision.codevisionbackend.graph.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Python-specific tree-sitter analyzer. Handles classes, functions,
 * imports, and decorated definitions.
 */
@Component
public class PythonAnalyzer extends LanguageAnalyzer {

    @Override
    public String languageName() {
        return "python";
    }

    @Override
    protected void analyzeTree(ParsedTree tree, KnowledgeGraph graph) {
        var sourceFile = tree.sourceFile();
        var fileNodeId = nodeId(sourceFile, sourceFile);
        graph.addNode(createNode(fileNodeId, KgNodeType.FILE, sourceFile,
                sourceFile, sourceFile, 1, 1, Map.of("language", "python")));

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

        switch (node.type()) {
            case "class_definition" -> processClass(node, parentNodeId, sourceFile, graph, visited);
            case "function_definition" -> processFunction(node, parentNodeId, sourceFile, graph, visited);
            case "decorated_definition" -> processDecorated(node, parentNodeId, sourceFile, graph, visited);
            case "import_statement" -> processImport(node, parentNodeId, sourceFile, graph);
            case "import_from_statement" -> processImportFrom(node, parentNodeId, sourceFile, graph);
            default -> {
                for (var child : node.children()) {
                    walkNode(child, parentNodeId, sourceFile, graph, visited);
                }
            }
        }
    }

    private void processClass(ParsedNode node, String parentNodeId,
                              String sourceFile, KnowledgeGraph graph,
                              Set<String> visited) {
        var name = extractName(node);
        var classNodeId = nodeId(sourceFile, name);
        graph.addNode(createNode(classNodeId, KgNodeType.CLASS, name,
                name, sourceFile, node.startLine(), node.endLine(), Map.of()));
        graph.addEdge(createEdge(parentNodeId, classNodeId, KgEdgeType.CONTAINS,
                "contains", sourceFile, node.startLine()));

        // Process superclasses
        var argList = findChild(node, "argument_list");
        if (argList != null) {
            for (var arg : argList.children()) {
                if ("identifier".equals(arg.type())) {
                    var superName = arg.text().trim();
                    var superNodeId = nodeId(sourceFile, "ref:" + superName);
                    graph.addEdge(createEdge(classNodeId, superNodeId, KgEdgeType.EXTENDS,
                            superName, sourceFile, arg.startLine()));
                }
            }
        }

        // Walk class body
        var body = findChild(node, "block");
        if (body != null) {
            for (var child : body.children()) {
                walkNode(child, classNodeId, sourceFile, graph, visited);
            }
        }
    }

    private void processFunction(ParsedNode node, String parentNodeId,
                                 String sourceFile, KnowledgeGraph graph,
                                 Set<String> visited) {
        var name = extractName(node);
        var funcNodeId = nodeId(sourceFile, parentNodeId + "." + name);
        graph.addNode(createNode(funcNodeId, KgNodeType.METHOD, name,
                name, sourceFile, node.startLine(), node.endLine(), Map.of()));
        graph.addEdge(createEdge(parentNodeId, funcNodeId, KgEdgeType.CONTAINS,
                "contains", sourceFile, node.startLine()));

        // Walk nested definitions inside the function
        var body = findChild(node, "block");
        if (body != null) {
            for (var child : body.children()) {
                walkNode(child, funcNodeId, sourceFile, graph, visited);
            }
        }
    }

    private void processDecorated(ParsedNode node, String parentNodeId,
                                  String sourceFile, KnowledgeGraph graph,
                                  Set<String> visited) {
        // Extract decorators
        var decorators = new ArrayList<String>();
        for (var child : node.children()) {
            if ("decorator".equals(child.type())) {
                decorators.add(child.text().trim());
            }
        }

        // Process the inner definition
        for (var child : node.children()) {
            if ("class_definition".equals(child.type()) || "function_definition".equals(child.type())) {
                walkNode(child, parentNodeId, sourceFile, graph, visited);

                // Add decorator annotations
                if (!decorators.isEmpty()) {
                    var innerName = extractName(child);
                    var innerNodeId = "class_definition".equals(child.type())
                            ? nodeId(sourceFile, innerName)
                            : nodeId(sourceFile, parentNodeId + "." + innerName);
                    for (var decorator : decorators) {
                        var decoratorId = nodeId(sourceFile, "decorator:" + decorator + ":" + node.startLine());
                        graph.addEdge(createEdge(decoratorId, innerNodeId, KgEdgeType.DECORATES,
                                decorator, sourceFile, node.startLine()));
                    }
                }
            }
        }
    }

    private void processImport(ParsedNode node, String parentNodeId,
                               String sourceFile, KnowledgeGraph graph) {
        var importText = node.text().trim();
        var importTargetId = nodeId(sourceFile, "import:" + importText);
        graph.addEdge(createEdge(parentNodeId, importTargetId, KgEdgeType.IMPORTS,
                importText, sourceFile, node.startLine()));
    }

    private void processImportFrom(ParsedNode node, String parentNodeId,
                                   String sourceFile, KnowledgeGraph graph) {
        var importText = node.text().trim();
        var importTargetId = nodeId(sourceFile, "import:" + importText);
        graph.addEdge(createEdge(parentNodeId, importTargetId, KgEdgeType.IMPORTS,
                importText, sourceFile, node.startLine()));
    }
}
