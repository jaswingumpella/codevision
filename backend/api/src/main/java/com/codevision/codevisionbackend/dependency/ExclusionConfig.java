package com.codevision.codevisionbackend.dependency;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Configurable exclusion patterns for filtering dependencies during resolution.
 * Supports glob-style patterns matching against groupId:artifactId coordinates.
 * Patterns are pre-compiled at construction time for performance.
 */
public final class ExclusionConfig {

    private final List<String> patterns;
    private final List<CompiledPattern> compiled;

    private static final ExclusionConfig DEFAULT = new ExclusionConfig(List.of(
            "org.springframework.*",
            "org.springframework.boot.*",
            "org.springframework.data.*",
            "org.springframework.security.*",
            "jakarta.*",
            "javax.*",
            "java.*",
            "org.junit.*",
            "org.mockito.*",
            "org.hamcrest.*",
            "org.slf4j.*",
            "ch.qos.logback.*",
            "org.apache.logging.*",
            "com.fasterxml.jackson.*",
            "org.projectlombok:lombok"
    ));

    private record CompiledPattern(Pattern regex, String base) {}

    public ExclusionConfig(List<String> patterns) {
        this.patterns = List.copyOf(patterns);
        this.compiled = patterns.stream()
                .map(p -> {
                    var regex = p.replace(".", "\\.").replace("*", ".*");
                    var base = p.endsWith(".*") ? p.substring(0, p.length() - 2) : null;
                    return new CompiledPattern(Pattern.compile(regex), base);
                })
                .toList();
    }

    public List<String> patterns() {
        return patterns;
    }

    public static ExclusionConfig defaults() {
        return DEFAULT;
    }

    public static ExclusionConfig none() {
        return new ExclusionConfig(List.of());
    }

    /**
     * Returns true if the given artifact should be excluded based on pattern matching.
     */
    public boolean isExcluded(ResolvedArtifact artifact) {
        if (artifact == null) {
            return true;
        }
        var coordinate = artifact.groupId() + ":" + artifact.artifactId();
        var groupId = artifact.groupId();
        for (var cp : compiled) {
            if (cp.regex.matcher(coordinate).matches()
                    || cp.regex.matcher(groupId).matches()
                    || (cp.base != null && (coordinate.equals(cp.base) || groupId.equals(cp.base)))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if the given groupId should be excluded.
     */
    public boolean isExcludedGroup(String groupId) {
        if (groupId == null) {
            return true;
        }
        for (var cp : compiled) {
            if (cp.regex.matcher(groupId).matches()
                    || (cp.base != null && groupId.equals(cp.base))) {
                return true;
            }
        }
        return false;
    }
}
