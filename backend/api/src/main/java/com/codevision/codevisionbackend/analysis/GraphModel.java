package com.codevision.codevisionbackend.analysis;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

/**
 * In-memory representation of the compiled artefact graph (classes, endpoints, sequences, and
 * dependencies). This model is serialized to {@code analysis.json}, feeds diagram writers, and
 * backs CSV/DB exports.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class GraphModel {

    private final Map<String, ClassNode> classes = new TreeMap<>();
    private final Map<String, SequenceNode> sequences = new TreeMap<>();
    private final List<SequenceUsage> sequenceUsages = new ArrayList<>();
    private final List<EndpointNode> endpoints = new ArrayList<>();
    private final List<MethodCallEdge> methodCallEdges = new ArrayList<>();
    private final List<DependencyEdge> dependencyEdges = new ArrayList<>();

    public Map<String, ClassNode> getClasses() {
        return classes;
    }

    public Map<String, SequenceNode> getSequences() {
        return sequences;
    }

    public List<SequenceUsage> getSequenceUsages() {
        return sequenceUsages;
    }

    public List<EndpointNode> getEndpoints() {
        return endpoints;
    }

    public List<MethodCallEdge> getMethodCallEdges() {
        return methodCallEdges;
    }

    public List<DependencyEdge> getDependencyEdges() {
        return dependencyEdges;
    }

    public void addClass(ClassNode node) {
        if (node == null || node.getName() == null) {
            return;
        }
        classes.put(node.getName(), node);
    }

    public void addSequence(SequenceNode sequenceNode) {
        if (sequenceNode == null || sequenceNode.getGeneratorName() == null) {
            return;
        }
        sequences.put(sequenceNode.getGeneratorName(), sequenceNode);
    }

    public void addSequenceUsage(SequenceUsage usage) {
        if (usage == null
                || usage.getClassName() == null
                || usage.getFieldName() == null
                || usage.getGeneratorName() == null) {
            return;
        }
        sequenceUsages.add(usage);
    }

    public void addEndpoint(EndpointNode endpointNode) {
        if (endpointNode == null) {
            return;
        }
        endpoints.add(endpointNode);
    }

    public void addMethodCallEdge(MethodCallEdge edge) {
        if (edge == null) {
            return;
        }
        methodCallEdges.add(edge);
    }

    public void addDependency(DependencyEdge dependencyEdge) {
        if (dependencyEdge == null) {
            return;
        }
        dependencyEdges.add(dependencyEdge);
    }

    public Map<String, Set<String>> buildAdjacencyMap() {
        Map<String, Set<String>> adjacency = new LinkedHashMap<>();
        for (DependencyEdge edge : dependencyEdges) {
            if (edge.getFromClass() == null || edge.getToClass() == null) {
                continue;
            }
            adjacency.computeIfAbsent(edge.getFromClass(), key -> new LinkedHashSet<>())
                    .add(edge.getToClass());
        }
        return adjacency;
    }

    public Map<String, Set<String>> buildCallAdjacency() {
        Map<String, Set<String>> adjacency = new LinkedHashMap<>();
        for (MethodCallEdge edge : methodCallEdges) {
            if (edge.getCallerClass() == null || edge.getCalleeClass() == null) {
                continue;
            }
            adjacency.computeIfAbsent(edge.getCallerClass(), key -> new LinkedHashSet<>())
                    .add(edge.getCalleeClass());
        }
        return adjacency;
    }

    public List<ClassNode> sortedClasses() {
        return classes.values().stream()
                .sorted(Comparator.comparing(ClassNode::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public List<EndpointNode> sortedEndpoints() {
        return endpoints.stream()
                .sorted(Comparator.comparing(EndpointNode::getControllerClass, Comparator.nullsLast(
                                String.CASE_INSENSITIVE_ORDER))
                        .thenComparing(EndpointNode::getPath, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();
    }

    public List<DependencyEdge> dependenciesOfKind(DependencyKind kind) {
        if (kind == null) {
            return List.of();
        }
        return dependencyEdges.stream()
                .filter(edge -> kind == edge.getKind())
                .toList();
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ClassNode {
        private String name;
        private String packageName;
        private String simpleName;
        private NodeKind kind = NodeKind.CLASS;
        private String superClass;
        private List<String> interfaces = new ArrayList<>();
        private List<String> annotations = new ArrayList<>();
        private List<String> stereotypes = new ArrayList<>();
        private List<FieldModel> fields = new ArrayList<>();
        private boolean entity;
        private String tableName;
        private Origin origin = Origin.BYTECODE;
        private Long sccId;
        private boolean inCycle;
        private String jarOrDirectory;
        private boolean springBean;

        public ClassNode copy() {
            ClassNode clone = new ClassNode();
            clone.setName(this.name);
            clone.setPackageName(this.packageName);
            clone.setSimpleName(this.simpleName);
            clone.setKind(this.kind);
            clone.setSuperClass(this.superClass);
            clone.setInterfaces(new ArrayList<>(this.interfaces));
            clone.setAnnotations(new ArrayList<>(this.annotations));
            clone.setStereotypes(new ArrayList<>(this.stereotypes));
            clone.setFields(this.fields.stream().map(FieldModel::copy).toList());
            clone.setEntity(this.entity);
            clone.setTableName(this.tableName);
            clone.setOrigin(this.origin);
            clone.setSccId(this.sccId);
            clone.setInCycle(this.inCycle);
            clone.setJarOrDirectory(this.jarOrDirectory);
            clone.setSpringBean(this.springBean);
            return clone;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getPackageName() {
            return packageName;
        }

        public void setPackageName(String packageName) {
            this.packageName = packageName;
        }

        public String getSimpleName() {
            return simpleName;
        }

        public void setSimpleName(String simpleName) {
            this.simpleName = simpleName;
        }

        public NodeKind getKind() {
            return kind;
        }

        public void setKind(NodeKind kind) {
            this.kind = kind;
        }

        public String getSuperClass() {
            return superClass;
        }

        public void setSuperClass(String superClass) {
            this.superClass = superClass;
        }

        public List<String> getInterfaces() {
            return interfaces;
        }

        public void setInterfaces(List<String> interfaces) {
            this.interfaces = interfaces == null ? new ArrayList<>() : new ArrayList<>(interfaces);
        }

        public List<String> getAnnotations() {
            return annotations;
        }

        public void setAnnotations(List<String> annotations) {
            this.annotations = annotations == null ? new ArrayList<>() : new ArrayList<>(annotations);
        }

        public List<String> getStereotypes() {
            return stereotypes;
        }

        public void setStereotypes(List<String> stereotypes) {
            this.stereotypes = stereotypes == null ? new ArrayList<>() : new ArrayList<>(stereotypes);
        }

        public List<FieldModel> getFields() {
            return fields;
        }

        public void setFields(List<FieldModel> fields) {
            this.fields = fields == null ? new ArrayList<>() : new ArrayList<>(fields);
        }

        public boolean isEntity() {
            return entity;
        }

        public void setEntity(boolean entity) {
            this.entity = entity;
        }

        public String getTableName() {
            return tableName;
        }

        public void setTableName(String tableName) {
            this.tableName = tableName;
        }

        public Origin getOrigin() {
            return origin;
        }

        public void setOrigin(Origin origin) {
            this.origin = origin;
        }

        public Long getSccId() {
            return sccId;
        }

        public void setSccId(Long sccId) {
            this.sccId = sccId;
        }

        public boolean isInCycle() {
            return inCycle;
        }

        public void setInCycle(boolean inCycle) {
            this.inCycle = inCycle;
        }

        public String getJarOrDirectory() {
            return jarOrDirectory;
        }

        public void setJarOrDirectory(String jarOrDirectory) {
            this.jarOrDirectory = jarOrDirectory;
        }

        public boolean isSpringBean() {
            return springBean;
        }

        public void setSpringBean(boolean springBean) {
            this.springBean = springBean;
        }
    }

    public enum Origin {
        SOURCE,
        BYTECODE,
        BOTH
    }

    public enum NodeKind {
        CLASS,
        INTERFACE,
        ENUM,
        RECORD
    }

    public enum DependencyKind {
        EXTENDS,
        IMPLEMENTS,
        CALL,
        INJECTION,
        FIELD
    }

    public enum EndpointType {
        HTTP,
        KAFKA,
        SCHEDULED
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class FieldModel {
        private String name;
        private String type;
        private List<String> annotations = new ArrayList<>();
        private boolean injected;
        private boolean relationship;

        public FieldModel copy() {
            FieldModel clone = new FieldModel();
            clone.setName(this.name);
            clone.setType(this.type);
            clone.setAnnotations(new ArrayList<>(this.annotations));
            clone.setInjected(this.injected);
            clone.setRelationship(this.relationship);
            return clone;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public List<String> getAnnotations() {
            return annotations;
        }

        public void setAnnotations(List<String> annotations) {
            this.annotations = annotations == null ? new ArrayList<>() : new ArrayList<>(annotations);
        }

        public boolean isInjected() {
            return injected;
        }

        public void setInjected(boolean injected) {
            this.injected = injected;
        }

        public boolean isRelationship() {
            return relationship;
        }

        public void setRelationship(boolean relationship) {
            this.relationship = relationship;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SequenceNode {
        private String generatorName;
        private String sequenceName;
        private Integer allocationSize;
        private Integer initialValue;

        public String getGeneratorName() {
            return generatorName;
        }

        public void setGeneratorName(String generatorName) {
            this.generatorName = generatorName;
        }

        public String getSequenceName() {
            return sequenceName;
        }

        public void setSequenceName(String sequenceName) {
            this.sequenceName = sequenceName;
        }

        public Integer getAllocationSize() {
            return allocationSize;
        }

        public void setAllocationSize(Integer allocationSize) {
            this.allocationSize = allocationSize;
        }

        public Integer getInitialValue() {
            return initialValue;
        }

        public void setInitialValue(Integer initialValue) {
            this.initialValue = initialValue;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SequenceUsage {
        private String className;
        private String fieldName;
        private String generatorName;

        public SequenceUsage() {}

        public SequenceUsage(String className, String fieldName, String generatorName) {
            this.className = className;
            this.fieldName = fieldName;
            this.generatorName = generatorName;
        }

        public String getClassName() {
            return className;
        }

        public void setClassName(String className) {
            this.className = className;
        }

        public String getFieldName() {
            return fieldName;
        }

        public void setFieldName(String fieldName) {
            this.fieldName = fieldName;
        }

        public String getGeneratorName() {
            return generatorName;
        }

        public void setGeneratorName(String generatorName) {
            this.generatorName = generatorName;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class EndpointNode {
        private EndpointType type = EndpointType.HTTP;
        private String httpMethod;
        private String path;
        private String controllerClass;
        private String controllerMethod;
        private String produces;
        private String consumes;
        private String framework;

        public EndpointType getType() {
            return type;
        }

        public void setType(EndpointType type) {
            this.type = type;
        }

        public String getHttpMethod() {
            return httpMethod;
        }

        public void setHttpMethod(String httpMethod) {
            this.httpMethod = httpMethod;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getControllerClass() {
            return controllerClass;
        }

        public void setControllerClass(String controllerClass) {
            this.controllerClass = controllerClass;
        }

        public String getControllerMethod() {
            return controllerMethod;
        }

        public void setControllerMethod(String controllerMethod) {
            this.controllerMethod = controllerMethod;
        }

        public String getProduces() {
            return produces;
        }

        public void setProduces(String produces) {
            this.produces = produces;
        }

        public String getConsumes() {
            return consumes;
        }

        public void setConsumes(String consumes) {
            this.consumes = consumes;
        }

        public String getFramework() {
            return framework;
        }

        public void setFramework(String framework) {
            this.framework = framework;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class MethodCallEdge {
        private String callerClass;
        private String callerMethod;
        private String callerDescriptor;
        private String calleeClass;
        private String calleeMethod;
        private String calleeDescriptor;

        public MethodCallEdge() {}

        public MethodCallEdge(
                String callerClass,
                String callerMethod,
                String callerDescriptor,
                String calleeClass,
                String calleeMethod,
                String calleeDescriptor) {
            this.callerClass = callerClass;
            this.callerMethod = callerMethod;
            this.callerDescriptor = callerDescriptor;
            this.calleeClass = calleeClass;
            this.calleeMethod = calleeMethod;
            this.calleeDescriptor = calleeDescriptor;
        }

        public String getCallerClass() {
            return callerClass;
        }

        public void setCallerClass(String callerClass) {
            this.callerClass = callerClass;
        }

        public String getCallerMethod() {
            return callerMethod;
        }

        public void setCallerMethod(String callerMethod) {
            this.callerMethod = callerMethod;
        }

        public String getCallerDescriptor() {
            return callerDescriptor;
        }

        public void setCallerDescriptor(String callerDescriptor) {
            this.callerDescriptor = callerDescriptor;
        }

        public String getCalleeClass() {
            return calleeClass;
        }

        public void setCalleeClass(String calleeClass) {
            this.calleeClass = calleeClass;
        }

        public String getCalleeMethod() {
            return calleeMethod;
        }

        public void setCalleeMethod(String calleeMethod) {
            this.calleeMethod = calleeMethod;
        }

        public String getCalleeDescriptor() {
            return calleeDescriptor;
        }

        public void setCalleeDescriptor(String calleeDescriptor) {
            this.calleeDescriptor = calleeDescriptor;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DependencyEdge {
        private DependencyKind kind;
        private String fromClass;
        private String toClass;
        private String label;

        public DependencyEdge() {}

        public DependencyEdge(DependencyKind kind, String fromClass, String toClass, String label) {
            this.kind = kind;
            this.fromClass = fromClass;
            this.toClass = toClass;
            this.label = label;
        }

        public DependencyKind getKind() {
            return kind;
        }

        public void setKind(DependencyKind kind) {
            this.kind = kind;
        }

        public String getFromClass() {
            return fromClass;
        }

        public void setFromClass(String fromClass) {
            this.fromClass = fromClass;
        }

        public String getToClass() {
            return toClass;
        }

        public void setToClass(String toClass) {
            this.toClass = toClass;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }
    }

    public static GraphModel empty() {
        return new GraphModel();
    }

    public static GraphModel ofSingleClass(ClassNode classNode) {
        GraphModel model = new GraphModel();
        if (classNode != null) {
            model.addClass(classNode);
        }
        return model;
    }

    public static boolean isUserPackage(String className, List<String> acceptPackages) {
        if (className == null) {
            return false;
        }
        if (acceptPackages == null || acceptPackages.isEmpty()) {
            return true;
        }
        return acceptPackages.stream().filter(Objects::nonNull).anyMatch(className::startsWith);
    }

    public Map<String, List<DependencyEdge>> dependenciesGroupedBySource() {
        Map<String, List<DependencyEdge>> grouped = new LinkedHashMap<>();
        for (DependencyEdge edge : dependencyEdges) {
            if (edge.getFromClass() == null) {
                continue;
            }
            grouped.computeIfAbsent(edge.getFromClass(), key -> new ArrayList<>()).add(edge);
        }
        for (List<DependencyEdge> edges : grouped.values()) {
            edges.sort(Comparator.comparing(DependencyEdge::getToClass, Comparator.nullsLast(
                    String.CASE_INSENSITIVE_ORDER)));
        }
        return grouped;
    }

    public Map<String, List<MethodCallEdge>> methodCallsGroupedBySource() {
        Map<String, List<MethodCallEdge>> grouped = new LinkedHashMap<>();
        for (MethodCallEdge edge : methodCallEdges) {
            if (edge.getCallerClass() == null) {
                continue;
            }
            grouped.computeIfAbsent(edge.getCallerClass(), key -> new ArrayList<>()).add(edge);
        }
        for (List<MethodCallEdge> edges : grouped.values()) {
            edges.sort(Comparator.comparing(MethodCallEdge::getCalleeClass, Comparator.nullsLast(
                    String.CASE_INSENSITIVE_ORDER)));
        }
        return grouped;
    }

    public Set<String> classNames() {
        return Collections.unmodifiableSet(classes.keySet());
    }
}
