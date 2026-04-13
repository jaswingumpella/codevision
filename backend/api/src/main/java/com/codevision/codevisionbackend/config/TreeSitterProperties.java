package com.codevision.codevisionbackend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Type-safe configuration properties for tree-sitter integration.
 */
@ConfigurationProperties(prefix = "codevision.tree-sitter")
public record TreeSitterProperties(
        boolean enabled,
        String nodePath,
        String scriptPath,
        int timeoutSeconds
) {
    public TreeSitterProperties() {
        this(true, "node", "scripts/tree-sitter-parse.js", 30);
    }
}
