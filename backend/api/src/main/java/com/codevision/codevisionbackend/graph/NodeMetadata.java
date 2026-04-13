package com.codevision.codevisionbackend.graph;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Rich, immutable metadata bag attached to every {@link KgNode}. Fields that
 * are irrelevant for a given node type are typically {@code null} or empty.
 *
 * @param visibility          access modifier (e.g. {@code "public"}, {@code "private"}, {@code "protected"}, {@code "package-private"})
 * @param modifiers           language modifiers (e.g. {@code "static"}, {@code "final"}, {@code "abstract"}, {@code "synchronized"})
 * @param annotations         annotations or decorators applied to this element
 * @param typeParameters      generic type parameter names (e.g. {@code ["T", "K extends Comparable"]})
 * @param returnType          return type of a method or function; {@code null} for non-callable nodes
 * @param parameterTypes      ordered list of parameter types for a callable element
 * @param thrownExceptions    declared checked exceptions (Java) or equivalent
 * @param documentation       extracted Javadoc / KDoc / docstring text
 * @param cyclomaticComplexity McCabe cyclomatic complexity; 0 when not applicable
 * @param cognitiveComplexity  cognitive complexity metric; 0 when not applicable
 * @param linesOfCode         lines of code for this element; 0 when not applicable
 * @param defaultValue        default value expression (e.g. annotation element defaults, field initialisers)
 * @param sourceFile          path to the originating source file
 * @param startLine           one-based start line within {@code sourceFile}; 0 when unknown
 * @param endLine             one-based end line within {@code sourceFile}; 0 when unknown
 * @param languageSpecific    arbitrary key-value pairs for language- or framework-specific data that does not fit the common schema
 */
public record NodeMetadata(
        String visibility,
        Set<String> modifiers,
        List<AnnotationValue> annotations,
        List<String> typeParameters,
        String returnType,
        List<String> parameterTypes,
        List<String> thrownExceptions,
        String documentation,
        int cyclomaticComplexity,
        int cognitiveComplexity,
        int linesOfCode,
        String defaultValue,
        String sourceFile,
        int startLine,
        int endLine,
        Map<String, Object> languageSpecific
) {}
