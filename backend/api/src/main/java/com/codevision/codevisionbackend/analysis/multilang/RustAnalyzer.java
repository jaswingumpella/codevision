package com.codevision.codevisionbackend.analysis.multilang;

import com.codevision.codevisionbackend.graph.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Rust-specific tree-sitter analyzer. Handles structs, enums, functions,
 * traits, impl blocks, and use declarations.
 */
@Component
public class RustAnalyzer extends LanguageAnalyzer {

    @Override
    public String languageName() {
        return "rust";
    }

    @Override
    protected void analyzeTree(ParsedTree tree, KnowledgeGraph graph) {
        var sourceFile = tree.sourceFile();
        var fileNodeId = nodeId(sourceFile, sourceFile);
        graph.addNode(createNode(fileNodeId, KgNodeType.FILE, sourceFile,
                sourceFile, sourceFile, 1, 1, Map.of("language", "rust")));

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
            case "struct_item" -> processStruct(node, parentNodeId, sourceFile, graph);
            case "enum_item" -> processEnum(node, parentNodeId, sourceFile, graph);
            case "function_item" -> processFunction(node, parentNodeId, sourceFile, graph);
            case "trait_item" -> processTrait(node, parentNodeId, sourceFile, graph, visited);
            case "impl_item" -> processImpl(node, parentNodeId, sourceFile, graph, visited);
            case "use_declaration" -> processUse(node, parentNodeId, sourceFile, graph);
            case "mod_item" -> processModule(node, parentNodeId, sourceFile, graph, visited);
            default -> {
                for (var child : node.children()) {
                    walkNode(child, parentNodeId, sourceFile, graph, visited);
                }
            }
        }
    }

    private void processStruct(ParsedNode node, String parentNodeId,
                               String sourceFile, KnowledgeGraph graph) {
        var name = extractName(node);
        var structNodeId = nodeId(sourceFile, name);
        graph.addNode(createNode(structNodeId, KgNodeType.STRUCT, name,
                name, sourceFile, node.startLine(), node.endLine(), Map.of()));
        graph.addEdge(createEdge(parentNodeId, structNodeId, KgEdgeType.CONTAINS,
                "contains", sourceFile, node.startLine()));

        // Process struct fields
        var fieldDeclList = findChild(node, "field_declaration_list");
        if (fieldDeclList != null) {
            for (var fieldDecl : findChildren(fieldDeclList, "field_declaration")) {
                var fieldName = findChild(fieldDecl, "field_identifier");
                if (fieldName != null) {
                    var fName = fieldName.text().trim();
                    var fieldNodeId = nodeId(sourceFile, name + "." + fName);
                    graph.addNode(createNode(fieldNodeId, KgNodeType.FIELD, fName,
                            name + "." + fName, sourceFile,
                            fieldDecl.startLine(), fieldDecl.endLine(), Map.of()));
                    graph.addEdge(createEdge(structNodeId, fieldNodeId, KgEdgeType.CONTAINS,
                            "field", sourceFile, fieldDecl.startLine()));
                }
            }
        }
    }

    private void processEnum(ParsedNode node, String parentNodeId,
                             String sourceFile, KnowledgeGraph graph) {
        var name = extractName(node);
        var enumNodeId = nodeId(sourceFile, name);
        graph.addNode(createNode(enumNodeId, KgNodeType.ENUM, name,
                name, sourceFile, node.startLine(), node.endLine(), Map.of()));
        graph.addEdge(createEdge(parentNodeId, enumNodeId, KgEdgeType.CONTAINS,
                "contains", sourceFile, node.startLine()));
    }

    private void processFunction(ParsedNode node, String parentNodeId,
                                 String sourceFile, KnowledgeGraph graph) {
        var name = extractName(node);
        var funcNodeId = nodeId(sourceFile, parentNodeId + "." + name);
        graph.addNode(createNode(funcNodeId, KgNodeType.FUNCTION, name,
                name, sourceFile, node.startLine(), node.endLine(), Map.of()));
        graph.addEdge(createEdge(parentNodeId, funcNodeId, KgEdgeType.CONTAINS,
                "contains", sourceFile, node.startLine()));
    }

    private void processTrait(ParsedNode node, String parentNodeId,
                              String sourceFile, KnowledgeGraph graph,
                              Set<String> visited) {
        var name = extractName(node);
        var traitNodeId = nodeId(sourceFile, name);
        graph.addNode(createNode(traitNodeId, KgNodeType.TRAIT, name,
                name, sourceFile, node.startLine(), node.endLine(), Map.of()));
        graph.addEdge(createEdge(parentNodeId, traitNodeId, KgEdgeType.CONTAINS,
                "contains", sourceFile, node.startLine()));

        // Process trait body for method signatures
        var body = findChild(node, "declaration_list");
        if (body != null) {
            for (var child : body.children()) {
                if ("function_signature_item".equals(child.type()) || "function_item".equals(child.type())) {
                    var methodName = extractName(child);
                    var methodNodeId = nodeId(sourceFile, name + "." + methodName);
                    graph.addNode(createNode(methodNodeId, KgNodeType.METHOD, methodName,
                            name + "." + methodName, sourceFile,
                            child.startLine(), child.endLine(), Map.of()));
                    graph.addEdge(createEdge(traitNodeId, methodNodeId, KgEdgeType.CONTAINS,
                            "contains", sourceFile, child.startLine()));
                }
            }
        }
    }

    private void processImpl(ParsedNode node, String parentNodeId,
                             String sourceFile, KnowledgeGraph graph,
                             Set<String> visited) {
        // Determine the target type (what we're implementing for)
        var typeIdent = findChild(node, "type_identifier");
        var typeName = typeIdent != null ? typeIdent.text().trim() : null;

        // Check if this is a trait impl (impl Trait for Type)
        String traitName = null;
        for (int i = 0; i < node.children().size(); i++) {
            var child = node.children().get(i);
            if ("for".equals(child.text().trim()) && i > 0) {
                traitName = typeName;
                // The type after "for" is the actual struct
                if (i + 1 < node.children().size()) {
                    var afterFor = node.children().get(i + 1);
                    typeName = afterFor.text().trim();
                }
                break;
            }
        }

        if (traitName != null && typeName != null) {
            // Add IMPLEMENTS edge: Type -> Trait
            var typeNodeId = nodeId(sourceFile, typeName);
            var traitNodeId = nodeId(sourceFile, traitName);
            graph.addEdge(createEdge(typeNodeId, traitNodeId, KgEdgeType.IMPLEMENTS,
                    typeName + " implements " + traitName, sourceFile, node.startLine()));
        }

        // Process impl body
        var body = findChild(node, "declaration_list");
        if (body != null) {
            var implParent = typeName != null ? nodeId(sourceFile, typeName) : parentNodeId;
            for (var child : body.children()) {
                if ("function_item".equals(child.type())) {
                    var methodName = extractName(child);
                    var qualifiedName = typeName != null ? typeName + "." + methodName : methodName;
                    var methodNodeId = nodeId(sourceFile, qualifiedName);
                    graph.addNode(createNode(methodNodeId, KgNodeType.METHOD, methodName,
                            qualifiedName, sourceFile,
                            child.startLine(), child.endLine(), Map.of()));
                    graph.addEdge(createEdge(implParent, methodNodeId, KgEdgeType.CONTAINS,
                            "contains", sourceFile, child.startLine()));
                }
            }
        }
    }

    private void processUse(ParsedNode node, String parentNodeId,
                            String sourceFile, KnowledgeGraph graph) {
        var useText = node.text().trim();
        var importTargetId = nodeId(sourceFile, "use:" + useText);
        graph.addEdge(createEdge(parentNodeId, importTargetId, KgEdgeType.IMPORTS,
                useText, sourceFile, node.startLine()));
    }

    private void processModule(ParsedNode node, String parentNodeId,
                               String sourceFile, KnowledgeGraph graph,
                               Set<String> visited) {
        var name = extractName(node);
        var modNodeId = nodeId(sourceFile, "mod:" + name);
        graph.addNode(createNode(modNodeId, KgNodeType.NAMESPACE, name,
                name, sourceFile, node.startLine(), node.endLine(), Map.of()));
        graph.addEdge(createEdge(parentNodeId, modNodeId, KgEdgeType.CONTAINS,
                "contains", sourceFile, node.startLine()));

        // Walk module body if inline
        var body = findChild(node, "declaration_list");
        if (body != null) {
            for (var child : body.children()) {
                walkNode(child, modNodeId, sourceFile, graph, visited);
            }
        }
    }
}
