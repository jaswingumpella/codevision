package com.codevision.codevisionbackend.analysis.multilang;

import com.codevision.codevisionbackend.graph.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * TypeScript/JavaScript-specific tree-sitter analyzer. Handles class declarations,
 * function declarations, arrow functions, interfaces, exports, imports, and
 * type aliases.
 */
@Component
public class TypeScriptAnalyzer extends LanguageAnalyzer {

    @Override
    public String languageName() {
        return "typescript";
    }

    @Override
    protected void analyzeTree(ParsedTree tree, KnowledgeGraph graph) {
        var sourceFile = tree.sourceFile();
        var fileNodeId = nodeId(sourceFile, sourceFile);
        graph.addNode(createNode(fileNodeId, KgNodeType.FILE, sourceFile,
                sourceFile, sourceFile, 1, 1, Map.of("language", tree.languageName())));

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
            case "class_declaration" -> processClass(node, parentNodeId, sourceFile, graph, visited);
            case "function_declaration" -> processFunction(node, parentNodeId, sourceFile, graph);
            case "arrow_function" -> processArrowFunction(node, parentNodeId, sourceFile, graph);
            case "method_definition" -> processMethod(node, parentNodeId, sourceFile, graph);
            case "interface_declaration" -> processInterface(node, parentNodeId, sourceFile, graph, visited);
            case "type_alias_declaration" -> processTypeAlias(node, parentNodeId, sourceFile, graph);
            case "export_statement" -> processExport(node, parentNodeId, sourceFile, graph, visited);
            case "import_statement" -> processImport(node, parentNodeId, sourceFile, graph);
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

        // Process class body children
        var body = findChild(node, "class_body");
        if (body != null) {
            for (var child : body.children()) {
                walkNode(child, classNodeId, sourceFile, graph, visited);
            }
        }
    }

    private void processFunction(ParsedNode node, String parentNodeId,
                                 String sourceFile, KnowledgeGraph graph) {
        var name = extractName(node);
        var funcNodeId = nodeId(sourceFile, parentNodeId + "." + name);
        graph.addNode(createNode(funcNodeId, KgNodeType.METHOD, name,
                name, sourceFile, node.startLine(), node.endLine(), Map.of()));
        graph.addEdge(createEdge(parentNodeId, funcNodeId, KgEdgeType.CONTAINS,
                "contains", sourceFile, node.startLine()));
    }

    private void processArrowFunction(ParsedNode node, String parentNodeId,
                                      String sourceFile, KnowledgeGraph graph) {
        var name = "arrow@" + node.startLine();
        var funcNodeId = nodeId(sourceFile, parentNodeId + "." + name);
        graph.addNode(createNode(funcNodeId, KgNodeType.LAMBDA, name,
                name, sourceFile, node.startLine(), node.endLine(), Map.of()));
        graph.addEdge(createEdge(parentNodeId, funcNodeId, KgEdgeType.CONTAINS,
                "contains", sourceFile, node.startLine()));
    }

    private void processMethod(ParsedNode node, String parentNodeId,
                               String sourceFile, KnowledgeGraph graph) {
        var name = extractName(node);
        var methodNodeId = nodeId(sourceFile, parentNodeId + "." + name);
        graph.addNode(createNode(methodNodeId, KgNodeType.METHOD, name,
                name, sourceFile, node.startLine(), node.endLine(), Map.of()));
        graph.addEdge(createEdge(parentNodeId, methodNodeId, KgEdgeType.CONTAINS,
                "contains", sourceFile, node.startLine()));
    }

    private void processInterface(ParsedNode node, String parentNodeId,
                                  String sourceFile, KnowledgeGraph graph,
                                  Set<String> visited) {
        var name = extractName(node);
        var ifaceNodeId = nodeId(sourceFile, name);
        graph.addNode(createNode(ifaceNodeId, KgNodeType.INTERFACE, name,
                name, sourceFile, node.startLine(), node.endLine(), Map.of()));
        graph.addEdge(createEdge(parentNodeId, ifaceNodeId, KgEdgeType.CONTAINS,
                "contains", sourceFile, node.startLine()));

        var body = findChild(node, "interface_body");
        if (body == null) body = findChild(node, "object_type");
        if (body != null) {
            for (var child : body.children()) {
                walkNode(child, ifaceNodeId, sourceFile, graph, visited);
            }
        }
    }

    private void processTypeAlias(ParsedNode node, String parentNodeId,
                                  String sourceFile, KnowledgeGraph graph) {
        var name = extractName(node);
        var typeNodeId = nodeId(sourceFile, name);
        graph.addNode(createNode(typeNodeId, KgNodeType.TYPE_ALIAS, name,
                name, sourceFile, node.startLine(), node.endLine(),
                Map.of("isTypeAlias", true)));
        graph.addEdge(createEdge(parentNodeId, typeNodeId, KgEdgeType.CONTAINS,
                "contains", sourceFile, node.startLine()));
    }

    private void processExport(ParsedNode node, String parentNodeId,
                               String sourceFile, KnowledgeGraph graph,
                               Set<String> visited) {
        // Process the inner declaration
        for (var child : node.children()) {
            if ("class_declaration".equals(child.type())
                    || "function_declaration".equals(child.type())
                    || "interface_declaration".equals(child.type())
                    || "type_alias_declaration".equals(child.type())) {

                walkNode(child, parentNodeId, sourceFile, graph, visited);

                // Add EXPORTS edge from the file to the declared entity
                var name = extractName(child);
                var declNodeId = switch (child.type()) {
                    case "class_declaration", "interface_declaration", "type_alias_declaration" ->
                            nodeId(sourceFile, name);
                    default -> nodeId(sourceFile, parentNodeId + "." + name);
                };
                graph.addEdge(createEdge(parentNodeId, declNodeId, KgEdgeType.EXPORTS,
                        "export " + name, sourceFile, node.startLine()));
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
}
