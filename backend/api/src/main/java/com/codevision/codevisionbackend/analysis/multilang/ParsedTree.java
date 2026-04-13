package com.codevision.codevisionbackend.analysis.multilang;

import java.util.List;

/**
 * Immutable wrapper around the result of parsing a single source file with
 * tree-sitter. Contains the full AST as a list of root-level {@link ParsedNode}s.
 *
 * @param sourceFile   path (relative or absolute) of the parsed file
 * @param languageName canonical language name matching {@link LanguageDefinition#name()}
 * @param sourceCode   raw source text that was parsed
 * @param rootNodes    top-level AST nodes (never {@code null})
 */
public record ParsedTree(
        String sourceFile,
        String languageName,
        String sourceCode,
        List<ParsedNode> rootNodes
) {}
