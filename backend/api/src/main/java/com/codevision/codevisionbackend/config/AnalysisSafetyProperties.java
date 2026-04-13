package com.codevision.codevisionbackend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Type-safe configuration properties for analysis safety limits.
 */
@ConfigurationProperties(prefix = "analysis.safety")
public record AnalysisSafetyProperties(
        long maxRuntimeSeconds,
        int maxHeapMb,
        int sequenceDiagramTimeoutSeconds
) {
    public AnalysisSafetyProperties() {
        this(600, 1500, 30);
    }
}
