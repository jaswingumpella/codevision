package com.codevision.codevisionbackend.analysis.multilang;

import java.util.List;

/**
 * Immutable representation of a single node in a tree-sitter AST.
 *
 * @param type        tree-sitter node type (e.g. "class_declaration", "function_definition")
 * @param text        source text spanned by this node
 * @param startLine   1-based start line in the source file
 * @param endLine     1-based end line in the source file
 * @param startColumn 0-based start column
 * @param endColumn   0-based end column
 * @param children    ordered child nodes (never {@code null})
 */
public record ParsedNode(
        String type,
        String text,
        int startLine,
        int endLine,
        int startColumn,
        int endColumn,
        List<ParsedNode> children
) {}
