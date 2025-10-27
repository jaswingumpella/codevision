package com.codevision.codevisionbackend.analyze.scanner;

import com.codevision.codevisionbackend.analyze.MetadataDump;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseProblemException;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MarkerAnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@Component
public class ApiScanner {

    private static final Logger log = LoggerFactory.getLogger(ApiScanner.class);
    private static final Set<String> SPRING_CONTROLLER_ANNOTATIONS = Set.of("RestController", "Controller");
    private static final Set<String> SPRING_ENDPOINT_ANNOTATIONS =
            Set.of("GetMapping", "PostMapping", "PutMapping", "DeleteMapping", "PatchMapping", "RequestMapping");
    private static final Set<String> SOAP_ENDPOINT_ANNOTATIONS = Set.of("Endpoint");
    private static final Set<String> SOAP_METHOD_ANNOTATIONS = Set.of("PayloadRoot", "SoapAction");
    private static final Set<String> JAXRS_HTTP_METHODS = Set.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS", "HEAD");
    private static final String JAXRS_PATH = "Path";
    private static final String SERVLET_BASE_CLASS = "HttpServlet";
    private static final Set<String> IGNORED_DIRECTORIES =
            Set.of(".git", "target", "build", "node_modules", ".idea", ".gradle");
    private static final ParserConfiguration JAVA_PARSER_CONFIGURATION = new ParserConfiguration()
            .setLanguageLevel(ParserConfiguration.LanguageLevel.BLEEDING_EDGE);

    private final JavaParser javaParser;
    private final ObjectMapper yamlMapper;

    public ApiScanner() {
        this.javaParser = new JavaParser(JAVA_PARSER_CONFIGURATION);
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
    }

    public List<ApiEndpointRecord> scan(
            Path repoRoot, List<Path> moduleRoots, MetadataDump metadataDump) {
        if (repoRoot == null || moduleRoots == null || moduleRoots.isEmpty()) {
            return List.of();
        }

        Map<String, Map<String, List<SpringMapping>>> interfaceMappings = new HashMap<>();
        Set<Path> visitedSources = new HashSet<>();
        Map<String, List<OpenApiOperation>> openApiOperations = buildOpenApiOperationIndex(metadataDump);

        for (Path moduleRoot : moduleRoots) {
            Path sourceRoot = moduleRoot.resolve("src/main/java").toAbsolutePath().normalize();
            if (!Files.isDirectory(sourceRoot) || !visitedSources.add(sourceRoot)) {
                continue;
            }
            collectInterfaceMappings(sourceRoot, interfaceMappings);
        }

        Map<String, List<String>> servletMappings = collectServletMappings(moduleRoots);
        List<ApiEndpointRecord> endpoints = new ArrayList<>();
        for (Path moduleRoot : moduleRoots) {
            Path sourceRoot = moduleRoot.resolve("src/main/java").toAbsolutePath().normalize();
            if (!Files.isDirectory(sourceRoot)) {
                continue;
            }
            scanModule(
                    repoRoot,
                    sourceRoot,
                    interfaceMappings,
                    servletMappings,
                    metadataDump,
                    openApiOperations,
                    endpoints);
        }
        endpoints.addAll(buildOpenApiOnlyEndpoints(openApiOperations));
        return endpoints;
    }

    private void collectInterfaceMappings(Path sourceRoot, Map<String, Map<String, List<SpringMapping>>> interfaceMappings) {
        try (var paths = Files.walk(sourceRoot, Integer.MAX_VALUE, FileVisitOption.FOLLOW_LINKS)) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .forEach(path -> parseInterfaceMappings(path, interfaceMappings));
        } catch (IOException e) {
            log.warn("Failed traversing {} for interface mappings: {}", sourceRoot, e.getMessage());
        }
    }

    private void parseInterfaceMappings(Path sourceFile, Map<String, Map<String, List<SpringMapping>>> interfaceMappings) {
        try {
            Optional<CompilationUnit> unitOpt = javaParser.parse(sourceFile).getResult();
            if (unitOpt.isEmpty()) {
                return;
            }
            CompilationUnit unit = unitOpt.get();
            for (TypeDeclaration<?> type : unit.getTypes()) {
                if (!(type instanceof ClassOrInterfaceDeclaration declaration) || !declaration.isInterface()) {
                    continue;
                }
                String simpleName = declaration.getName().asString();
                Map<String, List<SpringMapping>> methodMappings = interfaceMappings.computeIfAbsent(
                        simpleName, key -> new HashMap<>());

                for (MethodDeclaration method : declaration.getMethods()) {
                    List<SpringMapping> mappings = collectSpringMappings(method, true);
                    if (!mappings.isEmpty()) {
                        methodMappings.computeIfAbsent(method.getName().asString(), key -> new ArrayList<>())
                                .addAll(mappings);
                    }
                }
            }
        } catch (IOException | ParseProblemException ex) {
            log.debug("Failed parsing interface {}: {}", sourceFile, ex.getMessage());
        }
    }

    private void scanModule(
            Path repoRoot,
            Path sourceRoot,
            Map<String, Map<String, List<SpringMapping>>> interfaceMappings,
            Map<String, List<String>> servletMappings,
            MetadataDump metadataDump,
            Map<String, List<OpenApiOperation>> openApiOperations,
            List<ApiEndpointRecord> collector) {
        try (var paths = Files.walk(sourceRoot, Integer.MAX_VALUE, FileVisitOption.FOLLOW_LINKS)) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .forEach(path -> parseClass(
                            repoRoot, path, interfaceMappings, servletMappings, metadataDump, openApiOperations, collector));
        } catch (IOException e) {
            log.warn("Failed traversing {} for API scanning: {}", sourceRoot, e.getMessage());
        }
    }

    private void parseClass(
            Path repoRoot,
            Path sourceFile,
            Map<String, Map<String, List<SpringMapping>>> interfaceMappings,
            Map<String, List<String>> servletMappings,
            MetadataDump metadataDump,
            Map<String, List<OpenApiOperation>> openApiOperations,
            List<ApiEndpointRecord> collector) {
        try {
            Optional<CompilationUnit> unitOpt = javaParser.parse(sourceFile).getResult();
            if (unitOpt.isEmpty()) {
                return;
            }
            CompilationUnit unit = unitOpt.get();
            String packageName = unit.getPackageDeclaration()
                    .map(pkg -> pkg.getName().asString())
                    .orElse("");

            for (TypeDeclaration<?> type : unit.getTypes()) {
                if (!(type instanceof ClassOrInterfaceDeclaration declaration) || declaration.isInterface()) {
                    continue;
                }
                processType(
                        repoRoot,
                        packageName,
                        declaration,
                        interfaceMappings,
                        servletMappings,
                        metadataDump,
                        openApiOperations,
                        collector);
            }
        } catch (IOException | ParseProblemException ex) {
            log.debug("Failed parsing class {}: {}", sourceFile, ex.getMessage());
        }
    }

    private void processType(
            Path repoRoot,
            String packageName,
            ClassOrInterfaceDeclaration declaration,
            Map<String, Map<String, List<SpringMapping>>> interfaceMappings,
            Map<String, List<String>> servletMappings,
            MetadataDump metadataDump,
            Map<String, List<OpenApiOperation>> openApiOperations,
            List<ApiEndpointRecord> collector) {
        List<String> annotationNames = declaration.getAnnotations().stream()
                .map(annotation -> annotation.getName().getIdentifier())
                .map(name -> name.toLowerCase(Locale.ROOT))
                .toList();

        String fullyQualifiedName = declaration.getFullyQualifiedName()
                .orElseGet(() -> packageName == null || packageName.isBlank()
                        ? declaration.getNameAsString()
                        : packageName + "." + declaration.getNameAsString());

        if (isSpringController(annotationNames)) {
            collectSpringEndpoints(declaration, fullyQualifiedName, interfaceMappings, openApiOperations, collector);
            return;
        }

        if (isSoapEndpoint(annotationNames)) {
            collectSoapEndpoints(declaration, fullyQualifiedName, metadataDump, collector);
            return;
        }

        if (isJaxRsResource(annotationNames)) {
            collectJaxRsEndpoints(declaration, fullyQualifiedName, collector);
            return;
        }

        if (isServlet(declaration)) {
            collectServletEndpoints(declaration, fullyQualifiedName, servletMappings, collector);
        }
    }

    private boolean isSpringController(List<String> annotationNames) {
        return annotationNames.stream().anyMatch(name -> SPRING_CONTROLLER_ANNOTATIONS.contains(name));
    }

    private boolean isSoapEndpoint(List<String> annotationNames) {
        return annotationNames.stream().anyMatch(name -> SOAP_ENDPOINT_ANNOTATIONS.contains(name));
    }

    private boolean isJaxRsResource(List<String> annotationNames) {
        return annotationNames.stream().anyMatch(name -> name.equalsIgnoreCase(JAXRS_PATH));
    }

    private boolean isServlet(ClassOrInterfaceDeclaration declaration) {
        return declaration.getExtendedTypes().stream()
                .map(ClassOrInterfaceType::getName)
                .map(name -> name.getIdentifier().toLowerCase(Locale.ROOT))
                .anyMatch(name -> name.endsWith(SERVLET_BASE_CLASS.toLowerCase(Locale.ROOT)));
    }

    private void collectSpringEndpoints(
            ClassOrInterfaceDeclaration declaration,
            String fullyQualifiedName,
            Map<String, Map<String, List<SpringMapping>>> interfaceMappings,
            Map<String, List<OpenApiOperation>> openApiOperations,
            List<ApiEndpointRecord> collector) {
        List<String> basePaths = collectSpringMappings(declaration, false).stream()
                .map(SpringMapping::path)
                .filter(path -> path != null && !path.isBlank())
                .toList();
        if (basePaths.isEmpty()) {
            basePaths = List.of("");
        }

        List<String> interfaceNames = declaration.getImplementedTypes().stream()
                .map(type -> type.getName().getIdentifier())
                .toList();

        Map<String, List<ApiEndpointRecord.ApiSpecArtifactRecord>> specArtifactsByMethod = new HashMap<>();

        for (MethodDeclaration method : declaration.getMethods()) {
            List<SpringMapping> methodMappings = collectSpringMappings(method, true);
            if (methodMappings.isEmpty()) {
                methodMappings = resolveInterfaceMappings(interfaceMappings, interfaceNames, method.getNameAsString());
            }
            List<ApiEndpointRecord.ApiSpecArtifactRecord> specArtifacts = specArtifactsByMethod.computeIfAbsent(
                    method.getNameAsString(),
                    ignored -> buildOpenApiSpecArtifacts(openApiOperations, method.getNameAsString()));
            if (methodMappings.isEmpty()) {
                List<OpenApiOperation> openApiMatches = findOpenApiOperations(openApiOperations, method.getNameAsString());
                if (openApiMatches.isEmpty()) {
                    continue;
                }
                for (OpenApiOperation operation : openApiMatches) {
                    operation.markConsumed();
                    collector.add(new ApiEndpointRecord(
                            "REST",
                            operation.httpMethod,
                            operation.path,
                            fullyQualifiedName,
                            method.getNameAsString(),
                            specArtifacts));
                }
                continue;
            }
            for (SpringMapping mapping : methodMappings) {
                for (String basePath : basePaths) {
                    String path = combinePaths(basePath, mapping.path());
                    String httpMethod = mapping.httpMethod();
                    List<OpenApiOperation> openApiMatches = findOpenApiOperations(openApiOperations, method.getNameAsString());
                    openApiMatches.forEach(OpenApiOperation::markConsumed);
                    collector.add(new ApiEndpointRecord(
                            "REST",
                            httpMethod,
                            path,
                            fullyQualifiedName,
                            method.getNameAsString(),
                            specArtifacts));
                }
            }
        }
    }

    private List<SpringMapping> resolveInterfaceMappings(
            Map<String, Map<String, List<SpringMapping>>> interfaceMappings,
            List<String> interfaceNames,
            String methodName) {
        List<SpringMapping> resolved = new ArrayList<>();
        for (String iface : interfaceNames) {
            Map<String, List<SpringMapping>> methods = interfaceMappings.get(iface);
            if (methods == null) {
                continue;
            }
            List<SpringMapping> mappings = methods.get(methodName);
            if (mappings != null) {
                resolved.addAll(mappings);
            }
        }
        return resolved;
    }

    private void collectSoapEndpoints(
            ClassOrInterfaceDeclaration declaration,
            String fullyQualifiedName,
            MetadataDump metadataDump,
            List<ApiEndpointRecord> collector) {
        List<ApiEndpointRecord.ApiSpecArtifactRecord> artifacts = metadataDump != null
                ? metadataDump.wsdlDocuments().stream()
                        .map(doc -> new ApiEndpointRecord.ApiSpecArtifactRecord("WSDL", doc.fileName(), doc.fileName()))
                        .collect(Collectors.toList())
                : List.of();

        for (MethodDeclaration method : declaration.getMethods()) {
            List<AnnotationExpr> soapAnnotations = method.getAnnotations().stream()
                    .filter(annotation -> SOAP_METHOD_ANNOTATIONS.contains(annotation.getName().getIdentifier()))
                    .toList();
            if (soapAnnotations.isEmpty()) {
                continue;
            }
            String operation = extractSoapOperation(method.getNameAsString(), soapAnnotations);
            collector.add(new ApiEndpointRecord(
                    "SOAP",
                    null,
                    operation,
                    fullyQualifiedName,
                    method.getNameAsString(),
                    artifacts));
        }
    }

    private String extractSoapOperation(String defaultName, List<AnnotationExpr> annotations) {
        for (AnnotationExpr annotation : annotations) {
            if ("PayloadRoot".equals(annotation.getName().getIdentifier())) {
                if (annotation instanceof NormalAnnotationExpr normal) {
                    for (MemberValuePair pair : normal.getPairs()) {
                        if ("localPart".equals(pair.getName().asString())) {
                            return stripQuotes(pair.getValue().toString(), defaultName);
                        }
                    }
                }
            }
        }
        return defaultName;
    }

    private void collectJaxRsEndpoints(
            ClassOrInterfaceDeclaration declaration,
            String fullyQualifiedName,
            List<ApiEndpointRecord> collector) {
        List<String> basePaths = declaration.getAnnotations().stream()
                .filter(annotation -> annotation.getName().getIdentifier().equals(JAXRS_PATH))
                .map(this::extractFirstValue)
                .map(this::normalizeSegment)
                .toList();
        if (basePaths.isEmpty()) {
            basePaths = List.of("");
        }

        for (MethodDeclaration method : declaration.getMethods()) {
            String httpMethod = resolveJaxRsHttpMethod(method);
            if (httpMethod == null) {
                continue;
            }
            String methodPath = method.getAnnotations().stream()
                    .filter(annotation -> annotation.getName().getIdentifier().equals(JAXRS_PATH))
                    .map(this::extractFirstValue)
                    .map(this::normalizeSegment)
                    .findFirst()
                    .orElse("");

            for (String basePath : basePaths) {
                String combined = combinePaths(basePath, methodPath);
                collector.add(new ApiEndpointRecord(
                        "JAXRS",
                        httpMethod,
                        combined,
                        fullyQualifiedName,
                        method.getNameAsString(),
                        List.of()));
            }
        }
    }

    private String resolveJaxRsHttpMethod(MethodDeclaration method) {
        for (AnnotationExpr annotation : method.getAnnotations()) {
            String name = annotation.getName().getIdentifier().toUpperCase(Locale.ROOT);
            if (JAXRS_HTTP_METHODS.contains(name)) {
                return name;
            }
        }
        return null;
    }

    private void collectServletEndpoints(
            ClassOrInterfaceDeclaration declaration,
            String fullyQualifiedName,
            Map<String, List<String>> servletMappings,
            List<ApiEndpointRecord> collector) {
        List<String> urlPatterns = servletMappings.getOrDefault(
                fullyQualifiedName, servletMappings.get(declaration.getNameAsString()));
        if (urlPatterns == null || urlPatterns.isEmpty()) {
            urlPatterns = List.of("/" + declaration.getNameAsString());
        }

        for (MethodDeclaration method : declaration.getMethods()) {
            String methodName = method.getNameAsString();
            String httpMethod = resolveServletHttpMethod(methodName);
            if (httpMethod == null) {
                continue;
            }
            for (String pattern : urlPatterns) {
                ApiEndpointRecord.ApiSpecArtifactRecord artifact =
                        new ApiEndpointRecord.ApiSpecArtifactRecord("SERVLET_MAPPING", pattern, pattern);
                collector.add(new ApiEndpointRecord(
                        "SERVLET",
                        httpMethod,
                        pattern,
                        fullyQualifiedName,
                        methodName,
                        List.of(artifact)));
            }
        }
    }

    private String resolveServletHttpMethod(String methodName) {
        return switch (methodName) {
            case "doGet" -> "GET";
            case "doPost" -> "POST";
            case "doPut" -> "PUT";
            case "doDelete" -> "DELETE";
            case "doPatch" -> "PATCH";
            case "doHead" -> "HEAD";
            case "doOptions" -> "OPTIONS";
            case "doTrace" -> "TRACE";
            default -> null;
        };
    }

    private List<SpringMapping> collectSpringMappings(NodeWithAnnotations<?> node, boolean includeHttpMethod) {
        List<SpringMapping> mappings = new ArrayList<>();
        for (AnnotationExpr annotation : node.getAnnotations()) {
            String simpleName = annotation.getName().getIdentifier();
            if (SPRING_ENDPOINT_ANNOTATIONS.contains(simpleName)) {
                mappings.addAll(parseSpringAnnotation(annotation, includeHttpMethod));
            }
        }
        return mappings;
    }

    private List<SpringMapping> parseSpringAnnotation(AnnotationExpr annotation, boolean includeHttpMethod) {
        String name = annotation.getName().getIdentifier();
        List<String> paths = new ArrayList<>();
        List<String> methods = includeHttpMethod ? new ArrayList<>() : List.of("");

        if (annotation instanceof MarkerAnnotationExpr && !includeHttpMethod) {
            return List.of(new SpringMapping(null, ""));
        }

        if (annotation instanceof SingleMemberAnnotationExpr single) {
            paths.add(stripQuotes(single.getMemberValue().toString(), ""));
        } else if (annotation instanceof NormalAnnotationExpr normal) {
            for (MemberValuePair pair : normal.getPairs()) {
                String key = pair.getName().asString();
                if ("value".equals(key) || "path".equals(key)) {
                    paths.addAll(extractValues(pair.getValue()));
                } else if ("method".equals(key) && includeHttpMethod) {
                    methods = extractRequestMethods(pair.getValue());
                }
            }
        }

        if (paths.isEmpty()) {
            paths.add("");
        }
        if (includeHttpMethod && methods.isEmpty()) {
            methods = determineImplicitMethod(name);
        }
        if (methods.isEmpty()) {
            methods = List.of("");
        }

        List<SpringMapping> results = new ArrayList<>();
        for (String path : paths) {
            for (String method : methods) {
                results.add(new SpringMapping(method, normalizeSegment(path)));
            }
        }
        return results;
    }

    private List<String> determineImplicitMethod(String annotationName) {
        return switch (annotationName) {
            case "GetMapping" -> List.of("GET");
            case "PostMapping" -> List.of("POST");
            case "PutMapping" -> List.of("PUT");
            case "DeleteMapping" -> List.of("DELETE");
            case "PatchMapping" -> List.of("PATCH");
            default -> List.of();
        };
    }

    private List<String> extractValues(Expression expression) {
        if (expression instanceof ArrayInitializerExpr array) {
            return array.getValues().stream().map(expr -> stripQuotes(expr.toString(), "")).toList();
        }
        return List.of(stripQuotes(expression.toString(), ""));
    }

    private List<String> extractRequestMethods(Expression expression) {
        if (expression instanceof ArrayInitializerExpr array) {
            return array.getValues().stream().map(this::extractRequestMethod).toList();
        }
        return List.of(extractRequestMethod(expression));
    }

    private String extractRequestMethod(Expression expression) {
        if (expression instanceof FieldAccessExpr fieldAccess) {
            return fieldAccess.getName().getIdentifier();
        }
        if (expression instanceof NameExpr nameExpr) {
            return nameExpr.getName().getIdentifier();
        }
        return stripQuotes(expression.toString(), "");
    }

    private String extractFirstValue(AnnotationExpr annotation) {
        if (annotation instanceof SingleMemberAnnotationExpr single) {
            return stripQuotes(single.getMemberValue().toString(), "");
        }
        if (annotation instanceof NormalAnnotationExpr normal) {
            for (MemberValuePair pair : normal.getPairs()) {
                if ("value".equals(pair.getName().asString()) || "path".equals(pair.getName().asString())) {
                    List<String> values = extractValues(pair.getValue());
                    if (!values.isEmpty()) {
                        return values.get(0);
                    }
                }
            }
        }
        return "";
    }

    private String stripQuotes(String value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String trimmed = value.trim();
        if ((trimmed.startsWith("\"") && trimmed.endsWith("\""))
                || (trimmed.startsWith("'") && trimmed.endsWith("'"))) {
            return trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed.isEmpty() ? fallback : trimmed;
    }

    private String normalizeSegment(String segment) {
        if (segment == null || segment.isBlank()) {
            return "";
        }
        String trimmed = segment.trim();
        if (!trimmed.startsWith("/")) {
            trimmed = "/" + trimmed;
        }
        if (trimmed.length() > 1 && trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private String combinePaths(String base, String method) {
        String normalizedBase = normalizeSegment(base);
        String normalizedMethod = normalizeSegment(method);
        if (normalizedBase.isBlank() && normalizedMethod.isBlank()) {
            return "/";
        }
        if (normalizedBase.isBlank()) {
            return normalizedMethod;
        }
        if (normalizedMethod.isBlank()) {
            return normalizedBase;
        }
        if (normalizedBase.endsWith("/") && normalizedMethod.startsWith("/")) {
            return normalizedBase + normalizedMethod.substring(1);
        }
        if (!normalizedBase.endsWith("/") && !normalizedMethod.startsWith("/")) {
            return normalizedBase + "/" + normalizedMethod.substring(1);
        }
        return normalizedBase + normalizedMethod;
    }

    private Map<String, List<String>> collectServletMappings(List<Path> moduleRoots) {
        Map<String, List<String>> mappings = new HashMap<>();
        for (Path moduleRoot : moduleRoots) {
            Path webXml = moduleRoot.resolve("src/main/webapp/WEB-INF/web.xml");
            if (!Files.exists(webXml)) {
                continue;
            }
            try {
                parseWebXml(webXml, mappings);
            } catch (Exception ex) {
                log.debug("Failed to parse web.xml at {}: {}", webXml, ex.getMessage());
            }
        }
        return mappings;
    }

    private void parseWebXml(Path webXml, Map<String, List<String>> mappings) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        try (InputStream input = Files.newInputStream(webXml)) {
            Document document = builder.parse(input);
            document.getDocumentElement().normalize();

            Map<String, String> servletNameToClass = new HashMap<>();
            NodeList servletNodes = document.getElementsByTagName("servlet");
            for (int i = 0; i < servletNodes.getLength(); i++) {
                Node node = servletNodes.item(i);
                if (!(node instanceof Element element)) {
                    continue;
                }
                String servletName = getTextContent(element, "servlet-name");
                String servletClass = getTextContent(element, "servlet-class");
                if (servletName != null && servletClass != null) {
                    servletNameToClass.put(servletName, servletClass);
                }
            }

            NodeList mappingNodes = document.getElementsByTagName("servlet-mapping");
            for (int i = 0; i < mappingNodes.getLength(); i++) {
                Node node = mappingNodes.item(i);
                if (!(node instanceof Element element)) {
                    continue;
                }
                String servletName = getTextContent(element, "servlet-name");
                String urlPattern = getTextContent(element, "url-pattern");
                if (servletName == null || urlPattern == null) {
                    continue;
                }
                String servletClass = servletNameToClass.get(servletName);
                if (servletClass != null) {
                    mappings.computeIfAbsent(servletClass, key -> new ArrayList<>()).add(urlPattern);
                }
            }
        }
    }

    private String getTextContent(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes == null || nodes.getLength() == 0) {
            return null;
        }
        return nodes.item(0).getTextContent();
    }

    private Map<String, List<OpenApiOperation>> buildOpenApiOperationIndex(MetadataDump metadataDump) {
        if (metadataDump == null || metadataDump.openApiSpecs() == null) {
            return Collections.emptyMap();
        }
        Map<String, List<OpenApiOperation>> operations = new HashMap<>();
        for (MetadataDump.OpenApiSpec spec : metadataDump.openApiSpecs()) {
            if (spec == null || spec.content() == null || spec.content().isBlank()) {
                continue;
            }
            try {
                JsonNode root = yamlMapper.readTree(spec.content());
                JsonNode paths = root.get("paths");
                if (paths == null || !paths.isObject()) {
                    continue;
                }
                paths.fields().forEachRemaining(pathEntry -> {
                    String path = pathEntry.getKey();
                    JsonNode methodsNode = pathEntry.getValue();
                    if (methodsNode == null || !methodsNode.isObject()) {
                        return;
                    }
                    methodsNode.fields().forEachRemaining(methodEntry -> {
                        String methodName = methodEntry.getKey().toLowerCase(Locale.ROOT);
                        if (!HTTP_METHODS.contains(methodName)) {
                            return;
                        }
                        JsonNode operationNode = methodEntry.getValue();
                        if (operationNode == null || !operationNode.isObject()) {
                            return;
                        }
                        JsonNode operationIdNode = operationNode.get("operationId");
                        if (operationIdNode == null || operationIdNode.asText().isBlank()) {
                            return;
                        }
                        String operationId = operationIdNode.asText();
                        OpenApiOperation operation = new OpenApiOperation(
                                operationId,
                                methodName.toUpperCase(Locale.ROOT),
                                path,
                                spec.fileName());
                        operations.computeIfAbsent(operation.normalizedOperationId(), key -> new ArrayList<>())
                                .add(operation);
                    });
                });
            } catch (Exception ex) {
                log.debug("Failed parsing OpenAPI spec {}: {}", spec.fileName(), ex.getMessage());
            }
        }
        return operations;
    }

    private List<OpenApiOperation> findOpenApiOperations(
            Map<String, List<OpenApiOperation>> operations, String methodName) {
        if (operations == null || operations.isEmpty() || methodName == null) {
            return List.of();
        }
        return operations.getOrDefault(methodName.toLowerCase(Locale.ROOT), List.of());
    }

    private List<ApiEndpointRecord.ApiSpecArtifactRecord> buildOpenApiSpecArtifacts(
            Map<String, List<OpenApiOperation>> operations,
            String methodName) {
        List<OpenApiOperation> matches = findOpenApiOperations(operations, methodName);
        if (matches.isEmpty()) {
            return List.of();
        }
        return matches.stream()
                .map(match -> new ApiEndpointRecord.ApiSpecArtifactRecord("OPENAPI", match.specFile, match.specFile))
                .distinct()
                .toList();
    }

    private List<ApiEndpointRecord> buildOpenApiOnlyEndpoints(Map<String, List<OpenApiOperation>> operations) {
        if (operations == null || operations.isEmpty()) {
            return List.of();
        }
        List<ApiEndpointRecord> records = new ArrayList<>();
        for (List<OpenApiOperation> list : operations.values()) {
            for (OpenApiOperation operation : list) {
                if (operation.consumed) {
                    continue;
                }
                records.add(new ApiEndpointRecord(
                        "REST",
                        operation.httpMethod,
                        operation.path,
                        "OpenAPI:" + operation.specFile,
                        operation.operationId,
                        List.of(new ApiEndpointRecord.ApiSpecArtifactRecord(
                                "OPENAPI", operation.specFile, operation.specFile))));
            }
        }
        return records;
    }

    private static final Set<String> HTTP_METHODS = Set.of("get", "post", "put", "delete", "patch", "head", "options");

    private static final class OpenApiOperation {
        private final String operationId;
        private final String httpMethod;
        private final String path;
        private final String specFile;
        private boolean consumed;

        private OpenApiOperation(String operationId, String httpMethod, String path, String specFile) {
            this.operationId = operationId;
            this.httpMethod = httpMethod;
            this.path = path;
            this.specFile = specFile;
        }

        private String normalizedOperationId() {
            return operationId.toLowerCase(Locale.ROOT);
        }

        private void markConsumed() {
            this.consumed = true;
        }
    }

    private record SpringMapping(String httpMethod, String path) {}
}
