package com.codevision.codevisionbackend.graph;

import java.util.Map;

/**
 * Immutable representation of a single annotation (or decorator) applied to a
 * code element. Captures both the annotation identity and its key/value
 * parameters so that downstream consumers can reason about framework
 * configuration without re-parsing source.
 *
 * @param name          simple name of the annotation (e.g. {@code "RestController"})
 * @param qualifiedName fully-qualified name (e.g. {@code "org.springframework.web.bind.annotation.RestController"})
 * @param parameters    annotation parameters keyed by element name; values are strings, numbers, enums, or nested maps
 */
public record AnnotationValue(
        String name,
        String qualifiedName,
        Map<String, Object> parameters
) {}
