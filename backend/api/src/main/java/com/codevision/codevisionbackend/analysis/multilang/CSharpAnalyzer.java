package com.codevision.codevisionbackend.analysis.multilang;

import com.codevision.codevisionbackend.graph.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * C#-specific tree-sitter analyzer. Handles C# AST node types and
 * maps them to the CodeVision knowledge graph model including namespaces,
 * classes, interfaces, structs, enums, records, methods, properties,
 * fields, using directives, and extension methods.
 */
@Component
public class CSharpAnalyzer extends LanguageAnalyzer {

    @Override
    public String languageName() {
        return "csharp";
    }

    @Override
    protected void analyzeTree(ParsedTree tree, KnowledgeGraph graph) {
        var sourceFile = tree.sourceFile();
        var fileNodeId = nodeId(sourceFile, sourceFile);
        graph.addNode(createNode(fileNodeId, KgNodeType.FILE, sourceFile,
                sourceFile, sourceFile, 1, 1, Map.of("language", "csharp")));

        String namespaceName = null;
        var visited = new HashSet<String>();

        for (var rootNode : tree.rootNodes()) {
            switch (rootNode.type()) {
                case "namespace_declaration" -> {
                    namespaceName = extractName(rootNode);
                    var nsNodeId = nodeId(sourceFile, namespaceName);
                    graph.addNode(createNode(nsNodeId, KgNodeType.PACKAGE, namespaceName,
                            namespaceName, sourceFile, rootNode.startLine(), rootNode.endLine(), Map.of()));
                    graph.addEdge(createEdge(nsNodeId, fileNodeId, KgEdgeType.CONTAINS,
                            "contains", sourceFile, rootNode.startLine()));

                    // Walk children inside namespace body
                    var body = findChild(rootNode, "declaration_list");
                    if (body != null) {
                        for (var child : body.children()) {
                            walkNode(child, nsNodeId, namespaceName, sourceFile, graph, visited);
                        }
                    }
                }
                case "using_directive" -> {
                    var importText = rootNode.text()
                            .replace("using", "").replace(";", "").trim();
                    var importTargetId = nodeId(sourceFile, "import:" + importText);
                    graph.addEdge(createEdge(fileNodeId, importTargetId, KgEdgeType.IMPORTS,
                            importText, sourceFile, rootNode.startLine()));
                }
                default -> walkNode(rootNode, fileNodeId, namespaceName, sourceFile, graph, visited);
            }
        }
    }

    private void walkNode(ParsedNode node, String parentNodeId, String namespaceName,
                          String sourceFile, KnowledgeGraph graph, Set<String> visited) {
        var visitKey = sourceFile + ":" + node.startLine() + ":" + node.startColumn() + ":" + node.type();
        if (!visited.add(visitKey)) {
            return;
        }

        switch (node.type()) {
            case "class_declaration" -> processClassLike(node, parentNodeId, namespaceName,
                    KgNodeType.CLASS, sourceFile, graph, visited);
            case "interface_declaration" -> processClassLike(node, parentNodeId, namespaceName,
                    KgNodeType.INTERFACE, sourceFile, graph, visited);
            case "struct_declaration" -> processClassLike(node, parentNodeId, namespaceName,
                    KgNodeType.STRUCT, sourceFile, graph, visited);
            case "enum_declaration" -> processClassLike(node, parentNodeId, namespaceName,
                    KgNodeType.ENUM, sourceFile, graph, visited);
            case "record_declaration" -> processClassLike(node, parentNodeId, namespaceName,
                    KgNodeType.RECORD, sourceFile, graph, visited);
            case "method_declaration" -> processMethod(node, parentNodeId,
                    sourceFile, graph);
            case "constructor_declaration" -> processConstructor(node, parentNodeId,
                    sourceFile, graph);
            case "property_declaration" -> processProperty(node, parentNodeId,
                    sourceFile, graph);
            case "field_declaration" -> processField(node, parentNodeId,
                    sourceFile, graph);
            default -> {
                for (var child : node.children()) {
                    walkNode(child, parentNodeId, namespaceName, sourceFile, graph, visited);
                }
            }
        }
    }

    private void processClassLike(ParsedNode node, String parentNodeId, String namespaceName,
                                  KgNodeType nodeType, String sourceFile,
                                  KnowledgeGraph graph, Set<String> visited) {
        var name = extractName(node);
        var qualifiedName = namespaceName != null ? namespaceName + "." + name : name;
        var classNodeId = nodeId(sourceFile, qualifiedName);

        var modifiers = extractModifiers(node);
        var metadata = new NodeMetadata(
                extractVisibility(modifiers), modifiers, List.of(), List.of(),
                null, List.of(), List.of(), null,
                0, 0, node.endLine() - node.startLine() + 1,
                null, sourceFile, node.startLine(), node.endLine(), Map.of()
        );
        graph.addNode(new KgNode(classNodeId, nodeType, name, qualifiedName,
                metadata, null, "SOURCE", provenance(sourceFile, node.startLine())));
        graph.addEdge(createEdge(parentNodeId, classNodeId, KgEdgeType.CONTAINS,
                "contains", sourceFile, node.startLine()));

        // Process base_list for extends/implements
        processBaseList(node, classNodeId, nodeType, sourceFile, graph);

        // Walk children inside declaration_list body
        var body = findChild(node, "declaration_list");
        if (body != null) {
            for (var child : body.children()) {
                walkNode(child, classNodeId, namespaceName, sourceFile, graph, visited);
            }
        }
    }

    private void processMethod(ParsedNode node, String parentNodeId,
                               String sourceFile, KnowledgeGraph graph) {
        var name = extractName(node);
        var qualifiedName = parentNodeId.contains("#") ?
                parentNodeId.substring(parentNodeId.indexOf('#') + 1) + "." + name : name;
        var methodNodeId = nodeId(sourceFile, qualifiedName);

        var modifiers = extractModifiers(node);
        var isExtensionMethod = isExtensionMethod(node, modifiers);
        var nodeType = isExtensionMethod ? KgNodeType.EXTENSION_FUNCTION : KgNodeType.METHOD;

        var metadata = new NodeMetadata(
                extractVisibility(modifiers), modifiers, List.of(), List.of(),
                null, List.of(), List.of(), null,
                0, 0, node.endLine() - node.startLine() + 1,
                null, sourceFile, node.startLine(), node.endLine(), Map.of()
        );
        graph.addNode(new KgNode(methodNodeId, nodeType, name, qualifiedName,
                metadata, null, "SOURCE", provenance(sourceFile, node.startLine())));
        graph.addEdge(createEdge(parentNodeId, methodNodeId, KgEdgeType.CONTAINS,
                "contains", sourceFile, node.startLine()));
    }

    private void processConstructor(ParsedNode node, String parentNodeId,
                                    String sourceFile, KnowledgeGraph graph) {
        var name = extractName(node);
        var qualifiedName = parentNodeId.contains("#") ?
                parentNodeId.substring(parentNodeId.indexOf('#') + 1) + ".<init>" : name + ".<init>";
        var ctorNodeId = nodeId(sourceFile, qualifiedName);

        graph.addNode(createNode(ctorNodeId, KgNodeType.CONSTRUCTOR, name,
                qualifiedName, sourceFile, node.startLine(), node.endLine(), Map.of()));
        graph.addEdge(createEdge(parentNodeId, ctorNodeId, KgEdgeType.CONTAINS,
                "contains", sourceFile, node.startLine()));
    }

    private void processProperty(ParsedNode node, String parentNodeId,
                                 String sourceFile, KnowledgeGraph graph) {
        var name = extractName(node);
        var qualifiedName = parentNodeId.contains("#") ?
                parentNodeId.substring(parentNodeId.indexOf('#') + 1) + "." + name : name;
        var fieldNodeId = nodeId(sourceFile, qualifiedName);

        graph.addNode(createNode(fieldNodeId, KgNodeType.FIELD, name,
                qualifiedName, sourceFile, node.startLine(), node.endLine(), Map.of()));
        graph.addEdge(createEdge(parentNodeId, fieldNodeId, KgEdgeType.CONTAINS,
                "contains", sourceFile, node.startLine()));
    }

    private void processField(ParsedNode node, String parentNodeId,
                              String sourceFile, KnowledgeGraph graph) {
        var declarator = findChild(node, "variable_declarator");
        var name = declarator != null ? extractName(declarator) : extractName(node);
        var qualifiedName = parentNodeId.contains("#") ?
                parentNodeId.substring(parentNodeId.indexOf('#') + 1) + "." + name : name;
        var fieldNodeId = nodeId(sourceFile, qualifiedName);

        graph.addNode(createNode(fieldNodeId, KgNodeType.FIELD, name,
                qualifiedName, sourceFile, node.startLine(), node.endLine(), Map.of()));
        graph.addEdge(createEdge(parentNodeId, fieldNodeId, KgEdgeType.CONTAINS,
                "contains", sourceFile, node.startLine()));
    }

    private void processBaseList(ParsedNode node, String classNodeId,
                                 KgNodeType nodeType, String sourceFile,
                                 KnowledgeGraph graph) {
        var baseList = findChild(node, "base_list");
        if (baseList == null) {
            return;
        }

        var baseTypes = baseList.children();
        var isFirstType = true;
        for (var child : baseTypes) {
            var typeName = child.text().trim();
            if (typeName.isEmpty() || ":".equals(typeName) || ",".equals(typeName)) {
                continue;
            }

            // In C#, first type in base_list could be a class (extends) or interface (implements).
            // Convention: interface names start with 'I' followed by uppercase letter.
            // For interfaces/structs, all base types are IMPLEMENTS.
            if (nodeType == KgNodeType.INTERFACE || nodeType == KgNodeType.STRUCT) {
                var refId = nodeId(sourceFile, "ref:" + typeName);
                graph.addEdge(createEdge(classNodeId, refId, KgEdgeType.IMPLEMENTS,
                        typeName, sourceFile, child.startLine()));
            } else if (isFirstType && !looksLikeInterface(typeName)) {
                // First type that doesn't look like an interface → EXTENDS
                var refId = nodeId(sourceFile, "ref:" + typeName);
                graph.addEdge(createEdge(classNodeId, refId, KgEdgeType.EXTENDS,
                        typeName, sourceFile, child.startLine()));
            } else {
                var refId = nodeId(sourceFile, "ref:" + typeName);
                graph.addEdge(createEdge(classNodeId, refId, KgEdgeType.IMPLEMENTS,
                        typeName, sourceFile, child.startLine()));
            }
            isFirstType = false;
        }
    }

    /**
     * Checks if a method is a C# extension method: static with a 'this' modifier on the first parameter.
     */
    private boolean isExtensionMethod(ParsedNode node, Set<String> modifiers) {
        if (!modifiers.contains("static")) {
            return false;
        }
        var paramList = findChild(node, "parameter_list");
        if (paramList == null || paramList.children().isEmpty()) {
            return false;
        }
        var firstParam = paramList.children().get(0);
        if ("parameter".equals(firstParam.type())) {
            return findChild(firstParam, "this") != null;
        }
        return false;
    }

    /**
     * C# convention: interface names start with 'I' followed by an uppercase letter.
     */
    private boolean looksLikeInterface(String typeName) {
        return typeName.length() >= 2 && typeName.charAt(0) == 'I'
                && Character.isUpperCase(typeName.charAt(1));
    }

    private Set<String> extractModifiers(ParsedNode node) {
        var modifiers = new LinkedHashSet<String>();
        var modNode = findChild(node, "modifiers");
        if (modNode != null) {
            for (var child : modNode.children()) {
                modifiers.add(child.text().trim());
            }
        }
        return modifiers;
    }

    private String extractVisibility(Set<String> modifiers) {
        if (modifiers.contains("public")) return "public";
        if (modifiers.contains("protected")) return "protected";
        if (modifiers.contains("private")) return "private";
        if (modifiers.contains("internal")) return "internal";
        return "internal"; // C# default visibility
    }
}
