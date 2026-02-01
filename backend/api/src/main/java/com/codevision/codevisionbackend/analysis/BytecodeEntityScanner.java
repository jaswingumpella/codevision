package com.codevision.codevisionbackend.analysis;

import com.codevision.codevisionbackend.analysis.GraphModel.ClassNode;
import com.codevision.codevisionbackend.analysis.GraphModel.DependencyEdge;
import com.codevision.codevisionbackend.analysis.GraphModel.DependencyKind;
import com.codevision.codevisionbackend.analysis.GraphModel.EndpointNode;
import com.codevision.codevisionbackend.analysis.GraphModel.EndpointType;
import com.codevision.codevisionbackend.analysis.GraphModel.FieldModel;
import com.codevision.codevisionbackend.analysis.GraphModel.NodeKind;
import com.codevision.codevisionbackend.analysis.GraphModel.SequenceNode;
import com.codevision.codevisionbackend.analysis.GraphModel.SequenceUsage;
import com.codevision.codevisionbackend.analysis.config.CompiledAnalysisProperties;
import com.codevision.codevisionbackend.analyze.scanner.AnalysisExclusions;
import io.github.classgraph.AnnotationInfo;
import io.github.classgraph.AnnotationParameterValue;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.FieldInfo;
import io.github.classgraph.MethodInfo;
import io.github.classgraph.ScanResult;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Scans compiled bytecode using ClassGraph to discover classes, endpoints, sequences, and field
 * relationships without classloading the analyzed project.
 */
@Component
public class BytecodeEntityScanner {

    private static final Logger log = LoggerFactory.getLogger(BytecodeEntityScanner.class);

    private static final Set<String> ENTITY_ANNOTATIONS = Set.of(
            "jakarta.persistence.Entity",
            "javax.persistence.Entity",
            "org.springframework.data.mongodb.core.mapping.Document");
    private static final Set<String> TABLE_ANNOTATIONS = Set.of(
            "jakarta.persistence.Table", "javax.persistence.Table");
    private static final Set<String> RELATIONSHIP_ANNOTATIONS = Set.of(
            "jakarta.persistence.OneToOne",
            "jakarta.persistence.OneToMany",
            "jakarta.persistence.ManyToOne",
            "jakarta.persistence.ManyToMany",
            "javax.persistence.OneToOne",
            "javax.persistence.OneToMany",
            "javax.persistence.ManyToOne",
            "javax.persistence.ManyToMany",
            "jakarta.persistence.JoinColumn",
            "jakarta.persistence.JoinTable",
            "javax.persistence.JoinColumn",
            "javax.persistence.JoinTable");
    private static final Set<String> INJECTION_ANNOTATIONS = Set.of(
            "org.springframework.beans.factory.annotation.Autowired",
            "jakarta.inject.Inject",
            "javax.inject.Inject",
            "jakarta.annotation.Resource",
            "javax.annotation.Resource");
    private static final Map<String, String> STEREOTYPE_MAP = Map.ofEntries(
            Map.entry("org.springframework.web.bind.annotation.RestController", "CONTROLLER"),
            Map.entry("org.springframework.stereotype.Controller", "CONTROLLER"),
            Map.entry("org.springframework.stereotype.Service", "SERVICE"),
            Map.entry("org.springframework.stereotype.Repository", "REPOSITORY"),
            Map.entry("org.springframework.stereotype.Component", "COMPONENT"),
            Map.entry("org.springframework.context.annotation.Configuration", "CONFIGURATION"));
    private static final Map<String, String> MAPPING_TO_METHOD = Map.ofEntries(
            Map.entry("org.springframework.web.bind.annotation.GetMapping", "GET"),
            Map.entry("org.springframework.web.bind.annotation.PostMapping", "POST"),
            Map.entry("org.springframework.web.bind.annotation.PutMapping", "PUT"),
            Map.entry("org.springframework.web.bind.annotation.DeleteMapping", "DELETE"),
            Map.entry("org.springframework.web.bind.annotation.PatchMapping", "PATCH"));
    private static final Set<String> REQUEST_MAPPING = Set.of("org.springframework.web.bind.annotation.RequestMapping");
    private static final String KAFKA_LISTENER = "org.springframework.kafka.annotation.KafkaListener";
    private static final String SCHEDULED = "org.springframework.scheduling.annotation.Scheduled";
    private static final String BEAN_ANNOTATION = "org.springframework.context.annotation.Bean";
    private static final String SEQUENCE_GENERATOR = "jakarta.persistence.SequenceGenerator";
    private static final String SEQUENCE_GENERATOR_LEGACY = "javax.persistence.SequenceGenerator";
    private static final String TABLE_GENERATOR = "jakarta.persistence.TableGenerator";
    private static final String TABLE_GENERATOR_LEGACY = "javax.persistence.TableGenerator";
    private static final String GENERIC_GENERATOR = "org.hibernate.annotations.GenericGenerator";
    private static final String GENERATED_VALUE = "jakarta.persistence.GeneratedValue";
    private static final String GENERATED_VALUE_LEGACY = "javax.persistence.GeneratedValue";

    private final CompiledAnalysisProperties properties;

    public BytecodeEntityScanner(CompiledAnalysisProperties properties) {
        this.properties = properties;
    }

    public GraphModel scan(ClasspathBuilder.ClasspathDescriptor descriptor, List<String> overridePackages) {
        List<String> acceptPackages = effectivePackages(overridePackages);
        GraphModel model = new GraphModel();
        ClassGraph graph = new ClassGraph()
                .overrideClasspath(descriptor.getClasspathString())
                .enableClassInfo()
                .enableFieldInfo()
                .enableAnnotationInfo()
                .enableMethodInfo()
                .ignoreClassVisibility();

        if (!acceptPackages.isEmpty()) {
            graph = graph.acceptPackages(acceptPackages.toArray(String[]::new));
        }

        try (ScanResult scanResult = graph.scan()) {
            for (ClassInfo classInfo : scanResult.getAllClasses()) {
                if (classInfo.isAnnotation() || classInfo.isAnonymousInnerClass()) {
                    continue;
                }
                String fqcn = classInfo.getName();
                if (!GraphModel.isUserPackage(fqcn, acceptPackages)) {
                    continue;
                }
                if (AnalysisExclusions.isMockClassName(fqcn)) {
                    continue;
                }
                ClassNode node = toClassNode(classInfo);
                model.addClass(node);
                addInheritanceDependencies(model, node);
                inspectFields(model, classInfo, node);
                inspectClassSequences(model, classInfo);
                inspectEndpoints(model, classInfo, node);
            }
        }
        log.info(
                "Bytecode scanner discovered {} classes, {} endpoints, {} sequences",
                model.getClasses().size(),
                model.getEndpoints().size(),
                model.getSequences().size());
        return model;
    }

    private List<String> effectivePackages(List<String> overridePackages) {
        if (overridePackages != null && !overridePackages.isEmpty()) {
            return overridePackages;
        }
        return properties.getAcceptPackages();
    }

    private ClassNode toClassNode(ClassInfo classInfo) {
        ClassNode node = new ClassNode();
        node.setName(classInfo.getName());
        node.setPackageName(classInfo.getPackageName());
        node.setSimpleName(classInfo.getSimpleName());
        node.setJarOrDirectory(classInfo.getClasspathElementURI() != null
                ? classInfo.getClasspathElementURI().getPath()
                : null);
        node.setKind(resolveKind(classInfo));
        if (classInfo.getSuperclass() != null) {
            node.setSuperClass(classInfo.getSuperclass().getName());
        }
        node.setInterfaces(classInfo.getInterfaces().stream()
                .map(ClassInfo::getName)
                .collect(Collectors.toCollection(ArrayList::new)));
        List<String> annotations = classInfo.getAnnotationInfo().stream()
                .map(AnnotationInfo::getName)
                .toList();
        node.setAnnotations(annotations);
        node.setStereotypes(resolveStereotypes(annotations));
        node.setSpringBean(node.getStereotypes().stream().anyMatch(st -> !st.equals("CONTROLLER")));
        if (annotations.stream().anyMatch(ENTITY_ANNOTATIONS::contains)) {
            node.setEntity(true);
            node.setTableName(resolveTableName(classInfo).orElse(null));
        }
        return node;
    }

    private NodeKind resolveKind(ClassInfo classInfo) {
        if (classInfo.isInterface()) {
            return NodeKind.INTERFACE;
        }
        if (classInfo.isEnum()) {
            return NodeKind.ENUM;
        }
        if (classInfo.isRecord()) {
            return NodeKind.RECORD;
        }
        return NodeKind.CLASS;
    }

    private List<String> resolveStereotypes(List<String> annotations) {
        if (annotations == null || annotations.isEmpty()) {
            return List.of();
        }
        Set<String> resolved = new HashSet<>();
        for (String annotation : annotations) {
            String stereotype = STEREOTYPE_MAP.get(annotation);
            if (stereotype != null) {
                resolved.add(stereotype);
            }
        }
        return List.copyOf(resolved);
    }

    private Optional<String> resolveTableName(ClassInfo classInfo) {
        return classInfo.getAnnotationInfo().stream()
                .filter(annotation -> TABLE_ANNOTATIONS.contains(annotation.getName()))
                .map(BytecodeEntityScanner::extractName)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();
    }

    private void addInheritanceDependencies(GraphModel model, ClassNode node) {
        if (node.getSuperClass() != null) {
            model.addDependency(new DependencyEdge(
                    DependencyKind.EXTENDS, node.getName(), node.getSuperClass(), "extends"));
        }
        for (String iface : node.getInterfaces()) {
            model.addDependency(new DependencyEdge(
                    DependencyKind.IMPLEMENTS, node.getName(), iface, "implements"));
        }
    }

    private void inspectFields(GraphModel model, ClassInfo classInfo, ClassNode node) {
        for (FieldInfo fieldInfo : classInfo.getFieldInfo()) {
            FieldModel fieldModel = new FieldModel();
            fieldModel.setName(fieldInfo.getName());
            fieldModel.setType(fieldInfo.getTypeSignatureOrTypeDescriptor().toString());
            List<String> annotations = fieldInfo.getAnnotationInfo().stream()
                    .map(AnnotationInfo::getName)
                    .toList();
            fieldModel.setAnnotations(annotations);
            boolean injected = annotations.stream().anyMatch(INJECTION_ANNOTATIONS::contains);
            fieldModel.setInjected(injected);
            boolean relationship = annotations.stream().anyMatch(RELATIONSHIP_ANNOTATIONS::contains);
            fieldModel.setRelationship(relationship);
            node.getFields().add(fieldModel);

            if (injected) {
                String targetType = normalizeType(fieldModel.getType());
                if (targetType != null) {
                    model.addDependency(new DependencyEdge(
                            DependencyKind.INJECTION, node.getName(), targetType, fieldModel.getName()));
                }
            }

            if (annotations.stream().anyMatch(ann -> ann.equals(GENERATED_VALUE) || ann.equals(GENERATED_VALUE_LEGACY))) {
                extractGeneratedSequenceUsage(model, classInfo, fieldInfo);
            }
        }
    }

    private void extractGeneratedSequenceUsage(GraphModel model, ClassInfo classInfo, FieldInfo fieldInfo) {
        for (AnnotationInfo annotationInfo : fieldInfo.getAnnotationInfo()) {
            if (!annotationInfo.getName().equals(GENERATED_VALUE)
                    && !annotationInfo.getName().equals(GENERATED_VALUE_LEGACY)) {
                continue;
            }
            AnnotationParameterValue generator = annotationInfo.getParameterValues().get("generator");
            if (generator == null || generator.getValue() == null) {
                continue;
            }
            String generatorName = generator.getValue().toString();
            model.addSequenceUsage(new SequenceUsage(classInfo.getName(), fieldInfo.getName(), generatorName));
        }
    }

    private String normalizeType(String rawType) {
        if (rawType == null || rawType.isBlank()) {
            return null;
        }
        int genericStart = rawType.indexOf('<');
        String base = genericStart >= 0 ? rawType.substring(0, genericStart) : rawType;
        base = base.replace('/', '.').replace("L", "").trim();
        if (!base.contains(".")) {
            return null;
        }
        return base;
    }

    private void inspectClassSequences(GraphModel model, ClassInfo classInfo) {
        for (AnnotationInfo annotationInfo : classInfo.getAnnotationInfo()) {
            switch (annotationInfo.getName()) {
                case SEQUENCE_GENERATOR, SEQUENCE_GENERATOR_LEGACY -> addSequence(model, annotationInfo);
                case TABLE_GENERATOR, TABLE_GENERATOR_LEGACY -> addSequence(model, annotationInfo);
                case GENERIC_GENERATOR -> addSequence(model, annotationInfo);
                default -> {}
            }
        }
    }

    private void addSequence(GraphModel model, AnnotationInfo annotationInfo) {
        SequenceNode node = new SequenceNode();
        extractName(annotationInfo).ifPresent(node::setGeneratorName);
        Object sequenceName = annotationInfo.getParameterValues().getValue("sequenceName");
        if (sequenceName != null) {
            node.setSequenceName(sequenceName.toString());
        }
        Integer allocationSize = extractInteger(annotationInfo, "allocationSize");
        if (allocationSize != null) {
            node.setAllocationSize(allocationSize);
        }
        Integer initialValue = extractInteger(annotationInfo, "initialValue");
        if (initialValue != null) {
            node.setInitialValue(initialValue);
        }
        if (node.getGeneratorName() != null) {
            model.addSequence(node);
        }
    }

    private Integer extractInteger(AnnotationInfo annotationInfo, String key) {
        Object value = annotationInfo.getParameterValues().getValue(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        return null;
    }

    private static Optional<String> extractName(AnnotationInfo annotationInfo) {
        AnnotationParameterValue value = annotationInfo.getParameterValues().get("name");
        if (value == null || value.getValue() == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(value.getValue().toString()).filter(str -> !str.isBlank());
    }

    private void inspectEndpoints(GraphModel model, ClassInfo classInfo, ClassNode node) {
        List<String> classMappings = extractRequestMappingPaths(classInfo.getAnnotationInfo());
        String classProduces = extractSingle(classInfo.getAnnotationInfo(), "produces").orElse(null);
        String classConsumes = extractSingle(classInfo.getAnnotationInfo(), "consumes").orElse(null);

        for (MethodInfo methodInfo : classInfo.getMethodInfo()) {
            List<String> methodMappings = extractRequestMappingPaths(methodInfo.getAnnotationInfo());
            String httpMethod = determineHttpMethod(methodInfo);
            if (!methodMappings.isEmpty() || httpMethod != null) {
                List<String> combinedMappings = combineMappings(classMappings, methodMappings);
                if (combinedMappings.isEmpty()) {
                    combinedMappings = classMappings.isEmpty() ? List.of("/") : classMappings;
                }
                String produces = extractSingle(methodInfo.getAnnotationInfo(), "produces").orElse(classProduces);
                String consumes = extractSingle(methodInfo.getAnnotationInfo(), "consumes").orElse(classConsumes);
                for (String mapping : combinedMappings) {
                    EndpointNode endpoint = new EndpointNode();
                    endpoint.setType(EndpointType.HTTP);
                    endpoint.setControllerClass(classInfo.getName());
                    endpoint.setControllerMethod(methodInfo.getName());
                    endpoint.setHttpMethod(httpMethod != null ? httpMethod : "ANY");
                    endpoint.setPath(mapping);
                    endpoint.setProduces(produces);
                    endpoint.setConsumes(consumes);
                    endpoint.setFramework("SPRING_MVC");
                    model.addEndpoint(endpoint);
                }
                continue;
            }

            if (methodInfo.getAnnotationInfo().stream().anyMatch(info -> info.getName().equals(KAFKA_LISTENER))) {
                EndpointNode endpointNode = new EndpointNode();
                endpointNode.setType(EndpointType.KAFKA);
                endpointNode.setControllerClass(classInfo.getName());
                endpointNode.setControllerMethod(methodInfo.getName());
                endpointNode.setPath(resolveKafkaTopics(methodInfo.getAnnotationInfo()));
                endpointNode.setFramework("SPRING_KAFKA");
                model.addEndpoint(endpointNode);
                continue;
            }

            if (methodInfo.getAnnotationInfo().stream().anyMatch(info -> info.getName().equals(SCHEDULED))) {
                EndpointNode endpointNode = new EndpointNode();
                endpointNode.setType(EndpointType.SCHEDULED);
                endpointNode.setControllerClass(classInfo.getName());
                endpointNode.setControllerMethod(methodInfo.getName());
                endpointNode.setPath(resolveSchedule(methodInfo.getAnnotationInfo()));
                endpointNode.setFramework("SPRING_SCHEDULER");
                model.addEndpoint(endpointNode);
            }
        }

        boolean hasBeanMethod = classInfo.getMethodInfo().stream()
                .flatMap(method -> method.getAnnotationInfo().stream())
                .anyMatch(annotation -> annotation.getName().equals(BEAN_ANNOTATION));
        if (hasBeanMethod) {
            node.getStereotypes().add("BEAN_FACTORY");
        }
    }

    private List<String> extractRequestMappingPaths(Collection<AnnotationInfo> annotations) {
        List<String> paths = new ArrayList<>();
        for (AnnotationInfo annotationInfo : annotations) {
            if (REQUEST_MAPPING.contains(annotationInfo.getName()) || MAPPING_TO_METHOD.containsKey(annotationInfo.getName())) {
                paths.addAll(extractPaths(annotationInfo));
            }
        }
        return paths;
    }

    private List<String> extractPaths(AnnotationInfo annotationInfo) {
        List<String> values = new ArrayList<>();
        Object valueAttr = annotationInfo.getParameterValues().getValue("value");
        Object pathAttr = annotationInfo.getParameterValues().getValue("path");
        values.addAll(convertToList(valueAttr));
        values.addAll(convertToList(pathAttr));
        if (values.isEmpty()) {
            values.add("/");
        }
        return values;
    }

    private List<String> convertToList(Object attribute) {
        if (attribute == null) {
            return List.of();
        }
        if (attribute instanceof String str) {
            return str.isBlank() ? List.of() : List.of(str);
        }
        if (attribute.getClass().isArray()) {
            Object[] array = (Object[]) attribute;
            return Arrays.stream(array)
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .filter(str -> !str.isBlank())
                    .toList();
        }
        if (attribute instanceof Collection<?> collection) {
            return collection.stream()
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .filter(str -> !str.isBlank())
                    .toList();
        }
        return List.of(attribute.toString());
    }

    private String determineHttpMethod(MethodInfo methodInfo) {
        for (AnnotationInfo annotationInfo : methodInfo.getAnnotationInfo()) {
            String httpMethod = MAPPING_TO_METHOD.get(annotationInfo.getName());
            if (httpMethod != null) {
                return httpMethod;
            }
            if (REQUEST_MAPPING.contains(annotationInfo.getName())) {
                Object methodAttr = annotationInfo.getParameterValues().getValue("method");
                if (methodAttr instanceof Object[] array && array.length > 0) {
                    return array[0].toString().replace("RequestMethod.", "");
                }
            }
        }
        return null;
    }

    private List<String> combineMappings(List<String> classMappings, List<String> methodMappings) {
        if (classMappings.isEmpty()) {
            return methodMappings;
        }
        if (methodMappings.isEmpty()) {
            return classMappings;
        }
        List<String> combined = new ArrayList<>();
        for (String prefix : classMappings) {
            for (String suffix : methodMappings) {
                String normalizedPrefix = prefix.endsWith("/") ? prefix.substring(0, prefix.length() - 1) : prefix;
                String normalizedSuffix = suffix.startsWith("/") ? suffix : "/" + suffix;
                combined.add(normalizedPrefix + normalizedSuffix);
            }
        }
        return combined;
    }

    private Optional<String> extractSingle(Collection<AnnotationInfo> annotations, String attribute) {
        for (AnnotationInfo annotationInfo : annotations) {
            Object value = annotationInfo.getParameterValues().getValue(attribute);
            if (value instanceof String str && !str.isBlank()) {
                return Optional.of(str);
            }
        }
        return Optional.empty();
    }

    private String resolveKafkaTopics(Collection<AnnotationInfo> annotations) {
        for (AnnotationInfo annotationInfo : annotations) {
            if (!annotationInfo.getName().equals(KAFKA_LISTENER)) {
                continue;
            }
            List<String> topics = convertToList(annotationInfo.getParameterValues().getValue("topics"));
            if (!topics.isEmpty()) {
                return "topics=" + String.join(",", topics);
            }
            Object topicPattern = annotationInfo.getParameterValues().getValue("topicPattern");
            if (topicPattern != null) {
                return "topicPattern=" + topicPattern;
            }
        }
        return "kafka-listener";
    }

    private String resolveSchedule(Collection<AnnotationInfo> annotations) {
        for (AnnotationInfo annotationInfo : annotations) {
            if (!annotationInfo.getName().equals(SCHEDULED)) {
                continue;
            }
            Object cron = annotationInfo.getParameterValues().getValue("cron");
            if (cron != null) {
                return "cron=" + cron;
            }
            Object fixedDelay = annotationInfo.getParameterValues().getValue("fixedDelayString");
            if (fixedDelay != null) {
                return "fixedDelay=" + fixedDelay;
            }
            Object fixedRate = annotationInfo.getParameterValues().getValue("fixedRateString");
            if (fixedRate != null) {
                return "fixedRate=" + fixedRate;
            }
        }
        return "schedule";
    }
}
