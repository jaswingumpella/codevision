package com.codevision.codevisionbackend.analysis.multilang;

import com.codevision.codevisionbackend.graph.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Java-specific tree-sitter analyzer. Handles Java AST node types and
 * maps them to the CodeVision knowledge graph model including packages,
 * classes, interfaces, enums, methods, constructors, fields, and annotations.
 */
@Component
public class JavaTreeSitterAnalyzer extends LanguageAnalyzer {

    @Override
    public String languageName() {
        return "java";
    }

    @Override
    protected void analyzeTree(ParsedTree tree, KnowledgeGraph graph) {
        var sourceFile = tree.sourceFile();
        var fileNodeId = nodeId(sourceFile, sourceFile);
        graph.addNode(createNode(fileNodeId, KgNodeType.FILE, sourceFile,
                sourceFile, sourceFile, 1, 1, Map.of("language", "java")));

        String packageName = null;
        var visited = new HashSet<String>();

        for (var rootNode : tree.rootNodes()) {
            if ("package_declaration".equals(rootNode.type())) {
                packageName = extractPackageName(rootNode);
                var pkgNodeId = nodeId(sourceFile, packageName);
                graph.addNode(createNode(pkgNodeId, KgNodeType.PACKAGE, packageName,
                        packageName, sourceFile, rootNode.startLine(), rootNode.endLine(), Map.of()));
                graph.addEdge(createEdge(pkgNodeId, fileNodeId, KgEdgeType.CONTAINS,
                        "contains", sourceFile, rootNode.startLine()));
            } else if ("import_declaration".equals(rootNode.type())) {
                var importText = rootNode.text().replace("import", "").replace(";", "").trim();
                var importTargetId = nodeId(sourceFile, "import:" + importText);
                graph.addEdge(createEdge(fileNodeId, importTargetId, KgEdgeType.IMPORTS,
                        importText, sourceFile, rootNode.startLine()));
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
            case "class_declaration" -> processClassLike(node, parentNodeId, packageName,
                    KgNodeType.CLASS, sourceFile, graph, visited);
            case "interface_declaration" -> processClassLike(node, parentNodeId, packageName,
                    KgNodeType.INTERFACE, sourceFile, graph, visited);
            case "enum_declaration" -> processClassLike(node, parentNodeId, packageName,
                    KgNodeType.ENUM, sourceFile, graph, visited);
            case "record_declaration" -> processClassLike(node, parentNodeId, packageName,
                    KgNodeType.RECORD, sourceFile, graph, visited);
            case "annotation_declaration" -> processClassLike(node, parentNodeId, packageName,
                    KgNodeType.ANNOTATION_TYPE, sourceFile, graph, visited);
            case "method_declaration" -> processMethod(node, parentNodeId, packageName,
                    sourceFile, graph);
            case "constructor_declaration" -> processConstructor(node, parentNodeId, packageName,
                    sourceFile, graph);
            case "field_declaration" -> processField(node, parentNodeId, packageName,
                    sourceFile, graph);
            case "annotation" -> processAnnotation(node, parentNodeId, sourceFile, graph);
            default -> {
                for (var child : node.children()) {
                    walkNode(child, parentNodeId, packageName, sourceFile, graph, visited);
                }
            }
        }
    }

    private void processClassLike(ParsedNode node, String parentNodeId, String packageName,
                                  KgNodeType nodeType, String sourceFile,
                                  KnowledgeGraph graph, Set<String> visited) {
        var name = extractName(node);
        var qualifiedName = packageName != null ? packageName + "." + name : name;
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

        // Process extends/implements
        processSuperTypes(node, classNodeId, sourceFile, graph);

        // Walk class body children
        var body = findChild(node, "class_body");
        if (body == null) body = findChild(node, "enum_body");
        if (body == null) body = findChild(node, "interface_body");
        if (body == null) body = findChild(node, "record_declaration_body");
        if (body == null) body = findChild(node, "annotation_type_body");
        if (body != null) {
            for (var child : body.children()) {
                walkNode(child, classNodeId, packageName, sourceFile, graph, visited);
            }
        }
    }

    private void processMethod(ParsedNode node, String parentNodeId, String packageName,
                               String sourceFile, KnowledgeGraph graph) {
        var name = extractName(node);
        var qualifiedName = parentNodeId.contains("#") ?
                parentNodeId.substring(parentNodeId.indexOf('#') + 1) + "." + name : name;
        var methodNodeId = nodeId(sourceFile, qualifiedName);

        var modifiers = extractModifiers(node);
        var returnType = findChild(node, "type_identifier");
        var returnTypeStr = returnType != null ? returnType.text().trim() : "void";

        var metadata = new NodeMetadata(
                extractVisibility(modifiers), modifiers, List.of(), List.of(),
                returnTypeStr, List.of(), List.of(), null,
                0, 0, node.endLine() - node.startLine() + 1,
                null, sourceFile, node.startLine(), node.endLine(), Map.of()
        );
        graph.addNode(new KgNode(methodNodeId, KgNodeType.METHOD, name, qualifiedName,
                metadata, null, "SOURCE", provenance(sourceFile, node.startLine())));
        graph.addEdge(createEdge(parentNodeId, methodNodeId, KgEdgeType.CONTAINS,
                "contains", sourceFile, node.startLine()));
    }

    private void processConstructor(ParsedNode node, String parentNodeId, String packageName,
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

    private void processField(ParsedNode node, String parentNodeId, String packageName,
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

    private void processAnnotation(ParsedNode node, String parentNodeId,
                                   String sourceFile, KnowledgeGraph graph) {
        var name = extractName(node);
        var annotationNodeId = nodeId(sourceFile, "annotation:" + name + ":" + node.startLine());
        graph.addEdge(createEdge(annotationNodeId, parentNodeId, KgEdgeType.ANNOTATES,
                name, sourceFile, node.startLine()));
    }

    private void processSuperTypes(ParsedNode node, String classNodeId,
                                   String sourceFile, KnowledgeGraph graph) {
        var superclass = findChild(node, "superclass");
        if (superclass != null) {
            var superName = extractName(superclass);
            var superNodeId = nodeId(sourceFile, "ref:" + superName);
            graph.addEdge(createEdge(classNodeId, superNodeId, KgEdgeType.EXTENDS,
                    superName, sourceFile, superclass.startLine()));
        }

        var interfaces = findChild(node, "super_interfaces");
        if (interfaces != null) {
            for (var child : interfaces.children()) {
                if ("type_identifier".equals(child.type()) || "type_list".equals(child.type())) {
                    var ifaceName = child.text().trim();
                    var ifaceNodeId = nodeId(sourceFile, "ref:" + ifaceName);
                    graph.addEdge(createEdge(classNodeId, ifaceNodeId, KgEdgeType.IMPLEMENTS,
                            ifaceName, sourceFile, child.startLine()));
                }
            }
        }
    }

    private String extractPackageName(ParsedNode node) {
        var scopedId = findChild(node, "scoped_identifier");
        if (scopedId != null) {
            return scopedId.text().trim();
        }
        return node.text().replace("package", "").replace(";", "").trim();
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
        return "package-private";
    }
}
