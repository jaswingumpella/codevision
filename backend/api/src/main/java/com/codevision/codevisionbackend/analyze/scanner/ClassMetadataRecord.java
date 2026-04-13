package com.codevision.codevisionbackend.analyze.scanner;

import java.util.List;
import java.util.Map;

public record ClassMetadataRecord(
        String fullyQualifiedName,
        String packageName,
        String className,
        List<String> annotations,
        List<String> implementedInterfaces,
        String stereotype,
        SourceSet sourceSet,
        String relativePath,
        boolean userCode,
        String documentation,
        List<String> typeParameters,
        int linesOfCode,
        Map<String, MethodMetrics> methodMetrics) {

    /**
     * Backward-compatible constructor without enriched fields.
     */
    public ClassMetadataRecord(
            String fullyQualifiedName,
            String packageName,
            String className,
            List<String> annotations,
            List<String> implementedInterfaces,
            String stereotype,
            SourceSet sourceSet,
            String relativePath,
            boolean userCode) {
        this(fullyQualifiedName, packageName, className, annotations, implementedInterfaces,
                stereotype, sourceSet, relativePath, userCode, null, List.of(), 0, Map.of());
    }

    public record MethodMetrics(
            String methodName,
            int cyclomaticComplexity,
            int cognitiveComplexity,
            int linesOfCode,
            String documentation,
            List<String> parameterTypes,
            String returnType,
            List<String> thrownExceptions) {}

    public enum SourceSet {
        MAIN,
        TEST
    }
}
