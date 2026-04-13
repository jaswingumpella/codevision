package com.codevision.codevisionbackend.analysis.multilang;

import com.codevision.codevisionbackend.graph.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Kotlin-specific tree-sitter analyzer. Handles classes (including data classes),
 * functions, object declarations (singletons), interfaces, and imports.
 */
@Component
public class KotlinAnalyzer extends LanguageAnalyzer {

    @Override
    public String languageName() {
        return "kotlin";
    }

    @Override
    protected void analyzeTree(ParsedTree tree, KnowledgeGraph graph) {
        var sourceFile = tree.sourceFile();
        var fileNodeId = nodeId(sourceFile, sourceFile);
        graph.addNode(createNode(fileNodeId, KgNodeType.FILE, sourceFile,
                sourceFile, sourceFile, 1, 1, Map.of("language", "kotlin")));

        String packageName = null;
        var visited = new HashSet<String>();

        for (var rootNode : tree.rootNodes()) {
            if ("package_header".equals(rootNode.type())) {
                packageName = extractPackageName(rootNode);
                if (packageName != null && !packageName.isEmpty()) {
                    var pkgNodeId = nodeId(sourceFile, packageName);
                    graph.addNode(createNode(pkgNodeId, KgNodeType.PACKAGE, packageName,
                            packageName, sourceFile, rootNode.startLine(), rootNode.endLine(), Map.of()));
                    graph.addEdge(createEdge(pkgNodeId, fileNodeId, KgEdgeType.CONTAINS,
                            "contains", sourceFile, rootNode.startLine()));
                }
            } else if ("import_header".equals(rootNode.type()) || "import_list".equals(rootNode.type())) {
                processImport(rootNode, fileNodeId, sourceFile, graph);
            } else {
                walkNode(rootNode, fileNodeId, packageName, sourceFile, graph, visited);
            }
        }
    }

    private void walkNode(ParsedNode node, String parentNodeId, String packageName,
                          String sourceFile, KnowledgeGraph graph, Set<String> visited) {
        var visitKey = sourceFile + ":" + node.startLine() + ":" + node.startColumn() + ":" + node.type();
        if (!visited.add(visitKey)) {
            return;
        }

        switch (node.type()) {
            case "class_declaration" -> processClass(node, parentNodeId, packageName,
                    sourceFile, graph, visited);
            case "object_declaration" -> processObject(node, parentNodeId, packageName,
                    sourceFile, graph, visited);
            case "function_declaration" -> processFunction(node, parentNodeId, packageName,
                    sourceFile, graph);
            case "interface_declaration" -> processInterface(node, parentNodeId, packageName,
                    sourceFile, graph, visited);
            default -> {
                for (var child : node.children()) {
                    walkNode(child, parentNodeId, packageName, sourceFile, graph, visited);
                }
            }
        }
    }

    private void processClass(ParsedNode node, String parentNodeId, String packageName,
                              String sourceFile, KnowledgeGraph graph, Set<String> visited) {
        var name = extractName(node);
        var qualifiedName = packageName != null ? packageName + "." + name : name;
        var classNodeId = nodeId(sourceFile, qualifiedName);

        // Detect data class
        var isDataClass = node.text().trim().startsWith("data ");
        var langSpecific = isDataClass ? Map.<String, Object>of("isDataClass", true) : Map.<String, Object>of();

        graph.addNode(createNode(classNodeId, KgNodeType.CLASS, name,
                qualifiedName, sourceFile, node.startLine(), node.endLine(), langSpecific));
        graph.addEdge(createEdge(parentNodeId, classNodeId, KgEdgeType.CONTAINS,
                "contains", sourceFile, node.startLine()));

        // Process delegation_specifiers for extends/implements
        var delegationSpecifiers = findChild(node, "delegation_specifiers");
        if (delegationSpecifiers != null) {
            for (var spec : delegationSpecifiers.children()) {
                var specName = extractName(spec);
                var specNodeId = nodeId(sourceFile, "ref:" + specName);
                graph.addEdge(createEdge(classNodeId, specNodeId, KgEdgeType.EXTENDS,
                        specName, sourceFile, spec.startLine()));
            }
        }

        // Walk class body
        var body = findChild(node, "class_body");
        if (body != null) {
            for (var child : body.children()) {
                walkNode(child, classNodeId, packageName, sourceFile, graph, visited);
            }
        }
    }

    private void processObject(ParsedNode node, String parentNodeId, String packageName,
                               String sourceFile, KnowledgeGraph graph, Set<String> visited) {
        var name = extractName(node);
        var qualifiedName = packageName != null ? packageName + "." + name : name;
        var objNodeId = nodeId(sourceFile, qualifiedName);

        graph.addNode(createNode(objNodeId, KgNodeType.CLASS, name,
                qualifiedName, sourceFile, node.startLine(), node.endLine(),
                Map.of("isSingleton", true)));
        graph.addEdge(createEdge(parentNodeId, objNodeId, KgEdgeType.CONTAINS,
                "contains", sourceFile, node.startLine()));

        // Walk object body
        var body = findChild(node, "class_body");
        if (body != null) {
            for (var child : body.children()) {
                walkNode(child, objNodeId, packageName, sourceFile, graph, visited);
            }
        }
    }

    private void processFunction(ParsedNode node, String parentNodeId, String packageName,
                                 String sourceFile, KnowledgeGraph graph) {
        var name = extractName(node);
        var qualifiedName = parentNodeId.contains("#") ?
                parentNodeId.substring(parentNodeId.indexOf('#') + 1) + "." + name : name;
        var funcNodeId = nodeId(sourceFile, qualifiedName);

        graph.addNode(createNode(funcNodeId, KgNodeType.METHOD, name,
                qualifiedName, sourceFile, node.startLine(), node.endLine(), Map.of()));
        graph.addEdge(createEdge(parentNodeId, funcNodeId, KgEdgeType.CONTAINS,
                "contains", sourceFile, node.startLine()));
    }

    private void processInterface(ParsedNode node, String parentNodeId, String packageName,
                                  String sourceFile, KnowledgeGraph graph, Set<String> visited) {
        var name = extractName(node);
        var qualifiedName = packageName != null ? packageName + "." + name : name;
        var ifaceNodeId = nodeId(sourceFile, qualifiedName);

        graph.addNode(createNode(ifaceNodeId, KgNodeType.INTERFACE, name,
                qualifiedName, sourceFile, node.startLine(), node.endLine(), Map.of()));
        graph.addEdge(createEdge(parentNodeId, ifaceNodeId, KgEdgeType.CONTAINS,
                "contains", sourceFile, node.startLine()));

        var body = findChild(node, "class_body");
        if (body != null) {
            for (var child : body.children()) {
                walkNode(child, ifaceNodeId, packageName, sourceFile, graph, visited);
            }
        }
    }

    private void processImport(ParsedNode node, String parentNodeId,
                               String sourceFile, KnowledgeGraph graph) {
        if ("import_list".equals(node.type())) {
            for (var child : node.children()) {
                processImport(child, parentNodeId, sourceFile, graph);
            }
            return;
        }
        var importText = node.text().trim();
        var importTargetId = nodeId(sourceFile, "import:" + importText);
        graph.addEdge(createEdge(parentNodeId, importTargetId, KgEdgeType.IMPORTS,
                importText, sourceFile, node.startLine()));
    }

    private String extractPackageName(ParsedNode node) {
        var ident = findChild(node, "identifier");
        if (ident != null) {
            return ident.text().trim();
        }
        return node.text().replace("package", "").trim();
    }
}
