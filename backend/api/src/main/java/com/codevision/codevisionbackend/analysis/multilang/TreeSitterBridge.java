package com.codevision.codevisionbackend.analysis.multilang;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Abstraction layer between CodeVision and tree-sitter JNI.
 *
 * <p>The {@link #parseWithGrammar(String, String)} method is the extension point:
 * it can be overridden in tests or swapped with a real tree-sitter JNI binding
 * when available. The current default implementation produces a single root
 * {@link ParsedNode} that captures the entire source text, enabling downstream
 * analyzers to operate even before native bindings are integrated.</p>
 */
@Component
public class TreeSitterBridge {

    private final LanguageRegistry registry;

    public TreeSitterBridge(LanguageRegistry registry) {
        this.registry = registry;
    }

    /**
     * Parses source code by detecting its language from the filename.
     *
     * @param sourceCode the full source text
     * @param filename   the filename (or path) used for language detection
     * @return a parsed tree, or empty if the language is not supported
     */
    public Optional<ParsedTree> parse(String sourceCode, String filename) {
        var langOpt = registry.detect(filename);
        if (langOpt.isEmpty()) {
            return Optional.empty();
        }
        var lang = langOpt.get();
        var rootNodes = parseWithGrammar(sourceCode, lang.treeSitterLanguage());
        return Optional.of(new ParsedTree(filename, lang.name(), sourceCode, rootNodes));
    }

    /**
     * Returns whether the given filename has a recognised language extension.
     *
     * @param filename filename or path to check
     * @return {@code true} if the file can be parsed
     */
    public boolean isSupported(String filename) {
        return registry.detect(filename).isPresent();
    }

    /**
     * Low-level parse using a specific tree-sitter grammar.
     *
     * <p>Override this method to plug in real tree-sitter JNI bindings. The
     * default implementation produces a placeholder root node spanning the
     * entire source.</p>
     *
     * @param source   the source text to parse
     * @param language the tree-sitter grammar name
     * @return list of root-level AST nodes
     */
    protected List<ParsedNode> parseWithGrammar(String source, String language) {
        var lines = source.split("\n", -1);
        var lineCount = lines.length;
        var lastLineLength = lines.length > 0 ? lines[lines.length - 1].length() : 0;

        var root = new ParsedNode(
                "source_file",
                source,
                1,
                lineCount,
                0,
                lastLineLength,
                List.of()
        );
        return List.of(root);
    }
}
