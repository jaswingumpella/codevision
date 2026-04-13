package com.codevision.codevisionbackend.analysis.multilang;

import java.util.List;

/**
 * Immutable descriptor for a programming language supported by CodeVision's
 * multi-language analysis pipeline.
 *
 * @param name               canonical lowercase identifier (e.g. "java", "typescript")
 * @param displayName        human-readable label (e.g. "Java", "TypeScript")
 * @param fileExtensions     file extensions including the dot (e.g. ".java", ".ts")
 * @param treeSitterLanguage grammar name expected by tree-sitter
 */
public record LanguageDefinition(
        String name,
        String displayName,
        List<String> fileExtensions,
        String treeSitterLanguage
) {}
