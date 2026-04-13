package com.codevision.codevisionbackend.analysis.multilang;

import com.codevision.codevisionbackend.graph.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Go-specific tree-sitter analyzer. Handles functions, struct types,
 * interface types, method declarations (with receiver), and import paths.
 */
@Component
public class GoAnalyzer extends LanguageAnalyzer {

    @Override
    public String languageName() {
        return "go";
    }

    @Override
    protected void analyzeTree(ParsedTree tree, KnowledgeGraph graph) {
        var sourceFile = tree.sourceFile();
        var fileNodeId = nodeId(sourceFile, sourceFile);
        graph.addNode(createNode(fileNodeId, KgNodeType.FILE, sourceFile,
                sourceFile, sourceFile, 1, 1, Map.of("language", "go")));

        // Extract package name from package_clause
        String packageName = null;
        var visited = new HashSet<String>();

        for (var rootNode : tree.rootNodes()) {
            if ("package_clause".equals(rootNode.type())) {
                var pkgIdent = findChild(rootNode, "package_identifier");
                packageName = pkgIdent != null ? pkgIdent.text().trim()
                        : rootNode.text().replace("package", "").trim();
                var pkgNodeId = nodeId(sourceFile, packageName);
                graph.addNode(createNode(pkgNodeId, KgNodeType.PACKAGE, packageName,
                        packageName, sourceFile, rootNode.startLine(), rootNode.endLine(), Map.of()));
                graph.addEdge(createEdge(pkgNodeId, fileNodeId, KgEdgeType.CONTAINS,
                        "contains", sourceFile, rootNode.startLine()));
            }
        }

        for (var rootNode : tree.rootNodes()) {
            walkNode(rootNode, fileNodeId, packageName, sourceFile, graph, visited);
        }
    }

    private void walkNode(ParsedNode node, String parentNodeId, String packageName,
                          String sourceFile, KnowledgeGraph graph, Set<String> visited) {
        var visitKey = sourceFile + ":" + node.startLine() + ":" + node.startColumn() + ":" + node.type();
        if (!visited.add(visitKey)) {
            return;
        }

        switch (node.type()) {
            case "function_declaration" -> processFunction(node, parentNodeId, packageName,
                    sourceFile, graph);
            case "method_declaration" -> processMethodDeclaration(node, parentNodeId, packageName,
                    sourceFile, graph);
            case "type_declaration" -> processTypeDeclaration(node, parentNodeId, packageName,
                    sourceFile, graph);
            case "import_declaration" -> processImport(node, parentNodeId, sourceFile, graph);
            default -> {
                for (var child : node.children()) {
                    walkNode(child, parentNodeId, packageName, sourceFile, graph, visited);
                }
            }
        }
    }

    private void processFunction(ParsedNode node, String parentNodeId, String packageName,
                                 String sourceFile, KnowledgeGraph graph) {
        var name = extractName(node);
        var qualifiedName = packageName != null ? packageName + "." + name : name;
        var funcNodeId = nodeId(sourceFile, qualifiedName);
        graph.addNode(createNode(funcNodeId, KgNodeType.FUNCTION, name,
                qualifiedName, sourceFile, node.startLine(), node.endLine(), Map.of()));
        graph.addEdge(createEdge(parentNodeId, funcNodeId, KgEdgeType.CONTAINS,
                "contains", sourceFile, node.startLine()));
    }

    private void processMethodDeclaration(ParsedNode node, String parentNodeId, String packageName,
                                          String sourceFile, KnowledgeGraph graph) {
        var name = findChild(node, "field_identifier");
        var methodName = name != null ? name.text().trim() : extractName(node);

        // Extract receiver type
        var paramList = findChild(node, "parameter_list");
        String receiverType = null;
        if (paramList != null) {
            var paramDecl = findChild(paramList, "parameter_declaration");
            if (paramDecl != null) {
                var typeIdent = findChild(paramDecl, "type_identifier");
                if (typeIdent != null) {
                    receiverType = typeIdent.text().trim();
                } else {
                    var pointerType = findChild(paramDecl, "pointer_type");
                    if (pointerType != null) {
                        var innerType = findChild(pointerType, "type_identifier");
                        receiverType = innerType != null ? innerType.text().trim() : pointerType.text().trim();
                    }
                }
            }
        }

        var qualifiedName = packageName != null ? packageName + "." + methodName : methodName;
        if (receiverType != null) {
            qualifiedName = (packageName != null ? packageName + "." : "") + receiverType + "." + methodName;
        }
        var methodNodeId = nodeId(sourceFile, qualifiedName);
        graph.addNode(createNode(methodNodeId, KgNodeType.METHOD, methodName,
                qualifiedName, sourceFile, node.startLine(), node.endLine(),
                receiverType != null ? Map.of("receiver", receiverType) : Map.of()));
        graph.addEdge(createEdge(parentNodeId, methodNodeId, KgEdgeType.CONTAINS,
                "contains", sourceFile, node.startLine()));

        // Add CONTAINS edge from struct to method if receiver is known
        if (receiverType != null) {
            var structQualified = packageName != null ? packageName + "." + receiverType : receiverType;
            var structNodeId = nodeId(sourceFile, structQualified);
            graph.addEdge(createEdge(structNodeId, methodNodeId, KgEdgeType.CONTAINS,
                    "method receiver", sourceFile, node.startLine()));
        }
    }

    private void processTypeDeclaration(ParsedNode node, String parentNodeId, String packageName,
                                        String sourceFile, KnowledgeGraph graph) {
        for (var spec : node.children()) {
            if (!"type_spec".equals(spec.type())) continue;

            var name = extractName(spec);
            var qualifiedName = packageName != null ? packageName + "." + name : name;

            var structType = findChild(spec, "struct_type");
            var interfaceType = findChild(spec, "interface_type");

            if (structType != null) {
                var structNodeId = nodeId(sourceFile, qualifiedName);
                graph.addNode(createNode(structNodeId, KgNodeType.STRUCT, name,
                        qualifiedName, sourceFile, node.startLine(), node.endLine(), Map.of()));
                graph.addEdge(createEdge(parentNodeId, structNodeId, KgEdgeType.CONTAINS,
                        "contains", sourceFile, node.startLine()));

                // Process struct fields
                var fieldDeclList = findChild(structType, "field_declaration_list");
                if (fieldDeclList != null) {
                    for (var fieldDecl : findChildren(fieldDeclList, "field_declaration")) {
                        var fieldName = findChild(fieldDecl, "field_identifier");
                        if (fieldName != null) {
                            var fName = fieldName.text().trim();
                            var fieldNodeId = nodeId(sourceFile, qualifiedName + "." + fName);
                            graph.addNode(createNode(fieldNodeId, KgNodeType.FIELD, fName,
                                    qualifiedName + "." + fName, sourceFile,
                                    fieldDecl.startLine(), fieldDecl.endLine(), Map.of()));
                            graph.addEdge(createEdge(structNodeId, fieldNodeId, KgEdgeType.CONTAINS,
                                    "field", sourceFile, fieldDecl.startLine()));
                        }
                    }
                }
            } else if (interfaceType != null) {
                var ifaceNodeId = nodeId(sourceFile, qualifiedName);
                graph.addNode(createNode(ifaceNodeId, KgNodeType.INTERFACE, name,
                        qualifiedName, sourceFile, node.startLine(), node.endLine(), Map.of()));
                graph.addEdge(createEdge(parentNodeId, ifaceNodeId, KgEdgeType.CONTAINS,
                        "contains", sourceFile, node.startLine()));
            } else {
                // Type alias or other type declaration
                var typeNodeId = nodeId(sourceFile, qualifiedName);
                graph.addNode(createNode(typeNodeId, KgNodeType.TYPE_ALIAS, name,
                        qualifiedName, sourceFile, node.startLine(), node.endLine(), Map.of()));
                graph.addEdge(createEdge(parentNodeId, typeNodeId, KgEdgeType.CONTAINS,
                        "contains", sourceFile, node.startLine()));
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
