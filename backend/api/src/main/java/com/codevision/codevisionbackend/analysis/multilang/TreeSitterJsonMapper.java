package com.codevision.codevisionbackend.analysis.multilang;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility that converts tree-sitter JSON output produced by
 * {@code scripts/tree-sitter-parse.js} into a list of {@link ParsedNode}
 * records.
 *
 * <p>The expected JSON format per node:</p>
 * <pre>{@code
 * {
 *   "type": "class_declaration",
 *   "text": "public class Foo",
 *   "startLine": 1,
 *   "endLine": 10,
 *   "startColumn": 0,
 *   "endColumn": 1,
 *   "children": [ ... ]
 * }
 * }</pre>
 *
 * <p>Note: the Node.js script already converts 0-based row to 1-based
 * startLine, so no conversion is needed here.</p>
 */
public final class TreeSitterJsonMapper {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final int NESTING_DEPTH_LIMIT = 200;

    private TreeSitterJsonMapper() {}

    /**
     * Parses the JSON output from the tree-sitter Node.js bridge into a
     * list of root-level {@link ParsedNode} records.
     *
     * @param json the JSON string (an array of node objects)
     * @return list of parsed nodes
     * @throws IOException if the JSON is malformed
     */
    public static List<ParsedNode> parse(String json) throws IOException {
        if (json == null || json.isBlank()) {
            return List.of();
        }

        var rootArray = OBJECT_MAPPER.readTree(json);

        // Check if the output is an error object (has "error" field)
        if (rootArray.isObject() && rootArray.has("error")) {
            throw new IOException("tree-sitter parse error: " + rootArray.get("error").asText());
        }

        if (!rootArray.isArray()) {
            throw new IOException("Expected JSON array from tree-sitter, got: " + rootArray.getNodeType());
        }

        var result = new ArrayList<ParsedNode>(rootArray.size());
        for (var element : rootArray) {
            result.add(mapNode(element, 0));
        }
        return result;
    }

    private static ParsedNode mapNode(JsonNode jsonNode, int depth) throws IOException {
        if (depth > NESTING_DEPTH_LIMIT) {
            throw new IOException("tree-sitter JSON exceeds maximum nesting depth of " + NESTING_DEPTH_LIMIT);
        }

        var type = jsonNode.path("type").asText("unknown");
        var text = jsonNode.path("text").asText("");
        var startLine = jsonNode.path("startLine").asInt(1);
        var endLine = jsonNode.path("endLine").asInt(startLine);
        var startColumn = jsonNode.path("startColumn").asInt(0);
        var endColumn = jsonNode.path("endColumn").asInt(0);

        var childrenArray = jsonNode.path("children");
        var children = new ArrayList<ParsedNode>();
        if (childrenArray.isArray()) {
            for (var child : childrenArray) {
                children.add(mapNode(child, depth + 1));
            }
        }

        return new ParsedNode(type, text, startLine, endLine, startColumn, endColumn, children);
    }
}
