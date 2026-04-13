package com.codevision.codevisionbackend.graph;

import com.codevision.codevisionbackend.analysis.GraphModel;
import com.codevision.codevisionbackend.analysis.GraphModel.ClassNode;
import com.codevision.codevisionbackend.analysis.GraphModel.DependencyEdge;
import com.codevision.codevisionbackend.analysis.GraphModel.DependencyKind;
import com.codevision.codevisionbackend.analysis.GraphModel.EndpointNode;
import com.codevision.codevisionbackend.analysis.GraphModel.FieldModel;
import com.codevision.codevisionbackend.analysis.GraphModel.MethodCallEdge;
import com.codevision.codevisionbackend.analysis.GraphModel.NodeKind;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Bidirectional adapter between the legacy {@link GraphModel} and the new
 * {@link KnowledgeGraph} representation. All conversion methods are stateless
 * and side-effect-free.
 */
public final class GraphModelAdapter {

    private GraphModelAdapter() {
        // utility class
    }

    // ── Legacy -> Knowledge Graph ────────────────────────────────────────

    /**
     * Converts a legacy {@link GraphModel} into a {@link KnowledgeGraph}.
     *
     * <p>Mapping rules:</p>
     * <ul>
     *   <li>{@link ClassNode} becomes a {@link KgNode} whose type is derived
     *       from {@link NodeKind}.</li>
     *   <li>{@link DependencyEdge} becomes a {@link KgEdge} with a type
     *       derived from {@link DependencyKind}.</li>
     *   <li>{@link MethodCallEdge} becomes a {@link KgEdge} of type
     *       {@link KgEdgeType#CALLS}.</li>
     *   <li>{@link EndpointNode} becomes a {@link KgNode} of type
     *       {@link KgNodeType#ENDPOINT}.</li>
     * </ul>
     *
     * @param model the legacy graph model
     * @return a new knowledge graph; never {@code null}
     */
    public static KnowledgeGraph toKnowledgeGraph(GraphModel model) {
        if (model == null) {
            return new KnowledgeGraph();
        }

        KnowledgeGraph kg = new KnowledgeGraph();

        // Convert ClassNodes
        for (Map.Entry<String, ClassNode> entry : model.getClasses().entrySet()) {
            ClassNode cn = entry.getValue();
            kg.addNode(convertClassNode(cn));
        }

        // Convert EndpointNodes
        for (EndpointNode ep : model.getEndpoints()) {
            kg.addNode(convertEndpointNode(ep));
        }

        // Convert DependencyEdges
        for (DependencyEdge dep : model.getDependencyEdges()) {
            kg.addEdge(convertDependencyEdge(dep));
        }

        // Convert MethodCallEdges
        for (MethodCallEdge mce : model.getMethodCallEdges()) {
            kg.addEdge(convertMethodCallEdge(mce));
        }

        return kg;
    }

    // ── Knowledge Graph -> Legacy ────────────────────────────────────────

    /**
     * Converts a {@link KnowledgeGraph} back into a legacy {@link GraphModel}.
     * This is a best-effort reverse mapping; metadata that has no legacy
     * equivalent is dropped.
     *
     * @param kg the knowledge graph
     * @return a new legacy graph model; never {@code null}
     */
    public static GraphModel toGraphModel(KnowledgeGraph kg) {
        if (kg == null) {
            return GraphModel.empty();
        }

        GraphModel model = new GraphModel();

        // Convert nodes back to ClassNodes or EndpointNodes
        for (KgNode node : kg.getNodes().values()) {
            if (node.type() == KgNodeType.ENDPOINT) {
                model.addEndpoint(reverseEndpointNode(node));
            } else if (isTypeDeclaration(node.type())) {
                model.addClass(reverseClassNode(node));
            }
        }

        // Convert edges back
        for (KgEdge edge : kg.getEdges()) {
            if (edge.type() == KgEdgeType.CALLS) {
                model.addMethodCallEdge(reverseMethodCallEdge(edge));
            } else {
                DependencyEdge dep = reverseDependencyEdge(edge);
                if (dep != null) {
                    model.addDependency(dep);
                }
            }
        }

        return model;
    }

    // ── Forward helpers ──────────────────────────────────────────────────

    private static KgNode convertClassNode(ClassNode cn) {
        KgNodeType type = mapNodeKind(cn.getKind());
        String origin = cn.getOrigin() != null ? cn.getOrigin().name() : "BYTECODE";

        List<AnnotationValue> annotations = cn.getAnnotations() != null
                ? cn.getAnnotations().stream()
                    .map(a -> new AnnotationValue(a, a, Map.of()))
                    .toList()
                : List.of();

        Set<String> modifiers = new LinkedHashSet<>();
        if (cn.isEntity()) {
            modifiers.add("entity");
        }
        if (cn.isSpringBean()) {
            modifiers.add("spring-bean");
        }

        NodeMetadata metadata = new NodeMetadata(
                null,                   // visibility
                modifiers,              // modifiers
                annotations,            // annotations
                List.of(),              // typeParameters
                null,                   // returnType
                List.of(),              // parameterTypes
                List.of(),              // thrownExceptions
                null,                   // documentation
                0,                      // cyclomaticComplexity
                0,                      // cognitiveComplexity
                0,                      // linesOfCode
                null,                   // defaultValue
                null,                   // sourceFile
                0,                      // startLine
                0,                      // endLine
                cn.getTableName() != null ? Map.of("tableName", cn.getTableName()) : Map.of()
        );

        Provenance provenance = new Provenance(
                "GraphModelAdapter",
                cn.getJarOrDirectory(),
                0,
                ConfidenceLevel.EXTRACTED
        );

        return new KgNode(
                cn.getName(),
                type,
                cn.getSimpleName() != null ? cn.getSimpleName() : cn.getName(),
                cn.getName(),
                metadata,
                null,
                origin,
                provenance
        );
    }

    private static KgNode convertEndpointNode(EndpointNode ep) {
        String id = "endpoint:" + ep.getControllerClass() + "#" + ep.getControllerMethod()
                + ":" + ep.getHttpMethod() + ":" + ep.getPath();

        String label = (ep.getHttpMethod() != null ? ep.getHttpMethod() : "") + " " +
                (ep.getPath() != null ? ep.getPath() : "");

        NodeMetadata metadata = new NodeMetadata(
                null, Set.of(), List.of(), List.of(),
                null, List.of(), List.of(), null,
                0, 0, 0, null, null, 0, 0,
                Map.of(
                        "httpMethod", ep.getHttpMethod() != null ? ep.getHttpMethod() : "",
                        "path", ep.getPath() != null ? ep.getPath() : "",
                        "controllerClass", ep.getControllerClass() != null ? ep.getControllerClass() : "",
                        "controllerMethod", ep.getControllerMethod() != null ? ep.getControllerMethod() : "",
                        "produces", ep.getProduces() != null ? ep.getProduces() : "",
                        "consumes", ep.getConsumes() != null ? ep.getConsumes() : "",
                        "framework", ep.getFramework() != null ? ep.getFramework() : "",
                        "endpointType", ep.getType() != null ? ep.getType().name() : ""
                )
        );

        Provenance provenance = new Provenance(
                "GraphModelAdapter", null, 0, ConfidenceLevel.EXTRACTED
        );

        return new KgNode(
                id,
                KgNodeType.ENDPOINT,
                label.trim(),
                id,
                metadata,
                null,
                "SOURCE",
                provenance
        );
    }

    private static KgEdge convertDependencyEdge(DependencyEdge dep) {
        KgEdgeType type = mapDependencyKind(dep.getKind());

        Provenance provenance = new Provenance(
                "GraphModelAdapter", null, 0, ConfidenceLevel.EXTRACTED
        );

        return new KgEdge(
                UUID.randomUUID().toString(),
                type,
                dep.getFromClass(),
                dep.getToClass(),
                dep.getLabel(),
                ConfidenceLevel.EXTRACTED,
                provenance,
                Map.of()
        );
    }

    private static KgEdge convertMethodCallEdge(MethodCallEdge mce) {
        String label = (mce.getCallerMethod() != null ? mce.getCallerMethod() : "")
                + " -> "
                + (mce.getCalleeMethod() != null ? mce.getCalleeMethod() : "");

        Provenance provenance = new Provenance(
                "GraphModelAdapter", null, 0, ConfidenceLevel.EXTRACTED
        );

        return new KgEdge(
                UUID.randomUUID().toString(),
                KgEdgeType.CALLS,
                mce.getCallerClass(),
                mce.getCalleeClass(),
                label,
                ConfidenceLevel.EXTRACTED,
                provenance,
                Map.of(
                        "callerMethod", mce.getCallerMethod() != null ? mce.getCallerMethod() : "",
                        "callerDescriptor", mce.getCallerDescriptor() != null ? mce.getCallerDescriptor() : "",
                        "calleeMethod", mce.getCalleeMethod() != null ? mce.getCalleeMethod() : "",
                        "calleeDescriptor", mce.getCalleeDescriptor() != null ? mce.getCalleeDescriptor() : ""
                )
        );
    }

    // ── Reverse helpers ──────────────────────────────────────────────────

    private static ClassNode reverseClassNode(KgNode node) {
        ClassNode cn = new ClassNode();
        cn.setName(node.qualifiedName());
        cn.setSimpleName(node.name());
        cn.setKind(reverseNodeKind(node.type()));

        if (node.qualifiedName() != null && node.qualifiedName().contains(".")) {
            cn.setPackageName(node.qualifiedName().substring(0, node.qualifiedName().lastIndexOf('.')));
        }

        if (node.origin() != null) {
            try {
                cn.setOrigin(GraphModel.Origin.valueOf(node.origin()));
            } catch (IllegalArgumentException ignored) {
                cn.setOrigin(GraphModel.Origin.BYTECODE);
            }
        }

        if (node.metadata() != null) {
            if (node.metadata().annotations() != null) {
                cn.setAnnotations(node.metadata().annotations().stream()
                        .map(AnnotationValue::qualifiedName)
                        .toList());
            }
            if (node.metadata().modifiers() != null) {
                cn.setEntity(node.metadata().modifiers().contains("entity"));
                cn.setSpringBean(node.metadata().modifiers().contains("spring-bean"));
            }
            if (node.metadata().languageSpecific() != null) {
                Object tableName = node.metadata().languageSpecific().get("tableName");
                if (tableName != null) {
                    cn.setTableName(tableName.toString());
                }
            }
        }

        return cn;
    }

    private static EndpointNode reverseEndpointNode(KgNode node) {
        EndpointNode ep = new EndpointNode();

        if (node.metadata() != null && node.metadata().languageSpecific() != null) {
            Map<String, Object> ls = node.metadata().languageSpecific();
            ep.setHttpMethod(nonEmptyOrNull(ls.get("httpMethod")));
            ep.setPath(nonEmptyOrNull(ls.get("path")));
            ep.setControllerClass(nonEmptyOrNull(ls.get("controllerClass")));
            ep.setControllerMethod(nonEmptyOrNull(ls.get("controllerMethod")));
            ep.setProduces(nonEmptyOrNull(ls.get("produces")));
            ep.setConsumes(nonEmptyOrNull(ls.get("consumes")));
            ep.setFramework(nonEmptyOrNull(ls.get("framework")));

            Object endpointType = ls.get("endpointType");
            if (endpointType != null && !endpointType.toString().isEmpty()) {
                try {
                    ep.setType(GraphModel.EndpointType.valueOf(endpointType.toString()));
                } catch (IllegalArgumentException ignored) {
                    // keep default
                }
            }
        }

        return ep;
    }

    private static MethodCallEdge reverseMethodCallEdge(KgEdge edge) {
        MethodCallEdge mce = new MethodCallEdge();
        mce.setCallerClass(edge.sourceNodeId());
        mce.setCalleeClass(edge.targetNodeId());

        if (edge.properties() != null) {
            mce.setCallerMethod(nonEmptyOrNull(edge.properties().get("callerMethod")));
            mce.setCallerDescriptor(nonEmptyOrNull(edge.properties().get("callerDescriptor")));
            mce.setCalleeMethod(nonEmptyOrNull(edge.properties().get("calleeMethod")));
            mce.setCalleeDescriptor(nonEmptyOrNull(edge.properties().get("calleeDescriptor")));
        }

        return mce;
    }

    private static DependencyEdge reverseDependencyEdge(KgEdge edge) {
        DependencyKind kind = reverseDependencyKind(edge.type());
        if (kind == null) {
            return null;
        }
        return new DependencyEdge(
                kind,
                edge.sourceNodeId(),
                edge.targetNodeId(),
                edge.label()
        );
    }

    // ── Enum mapping ─────────────────────────────────────────────────────

    private static KgNodeType mapNodeKind(NodeKind kind) {
        if (kind == null) {
            return KgNodeType.CLASS;
        }
        return switch (kind) {
            case CLASS     -> KgNodeType.CLASS;
            case INTERFACE -> KgNodeType.INTERFACE;
            case ENUM      -> KgNodeType.ENUM;
            case RECORD    -> KgNodeType.RECORD;
        };
    }

    private static NodeKind reverseNodeKind(KgNodeType type) {
        return switch (type) {
            case INTERFACE -> NodeKind.INTERFACE;
            case ENUM      -> NodeKind.ENUM;
            case RECORD    -> NodeKind.RECORD;
            default        -> NodeKind.CLASS;
        };
    }

    private static KgEdgeType mapDependencyKind(DependencyKind kind) {
        if (kind == null) {
            return KgEdgeType.DEPENDS_ON;
        }
        return switch (kind) {
            case EXTENDS    -> KgEdgeType.EXTENDS;
            case IMPLEMENTS -> KgEdgeType.IMPLEMENTS;
            case CALL       -> KgEdgeType.CALLS;
            case INJECTION  -> KgEdgeType.INJECTS;
            case FIELD      -> KgEdgeType.READS_FIELD;
        };
    }

    private static DependencyKind reverseDependencyKind(KgEdgeType type) {
        return switch (type) {
            case EXTENDS    -> DependencyKind.EXTENDS;
            case IMPLEMENTS -> DependencyKind.IMPLEMENTS;
            case INJECTS    -> DependencyKind.INJECTION;
            case READS_FIELD, WRITES_FIELD -> DependencyKind.FIELD;
            default         -> null;
        };
    }

    private static boolean isTypeDeclaration(KgNodeType type) {
        return switch (type) {
            case CLASS, INTERFACE, ENUM, RECORD, STRUCT, TRAIT, PROTOCOL,
                 ANNOTATION_TYPE, TYPE_ALIAS, UNION -> true;
            default -> false;
        };
    }

    private static String nonEmptyOrNull(Object value) {
        if (value == null) {
            return null;
        }
        String s = value.toString();
        return s.isEmpty() ? null : s;
    }
}
