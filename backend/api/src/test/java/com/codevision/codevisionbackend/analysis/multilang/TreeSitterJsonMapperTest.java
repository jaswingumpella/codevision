package com.codevision.codevisionbackend.analysis.multilang;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class TreeSitterJsonMapperTest {

    @Nested
    class Given_ValidJsonArray {

        @Nested
        class When_Parsing {

            @Test
            void Then_ReturnsParsedNodes() throws IOException {
                var json = """
                    [
                      {
                        "type": "class_declaration",
                        "text": "public class Foo",
                        "startLine": 1,
                        "endLine": 10,
                        "startColumn": 0,
                        "endColumn": 1,
                        "children": []
                      }
                    ]
                    """;
                var nodes = TreeSitterJsonMapper.parse(json);
                assertEquals(1, nodes.size());
                var node = nodes.get(0);
                assertEquals("class_declaration", node.type());
                assertEquals("public class Foo", node.text());
                assertEquals(1, node.startLine());
                assertEquals(10, node.endLine());
                assertEquals(0, node.startColumn());
                assertEquals(1, node.endColumn());
                assertTrue(node.children().isEmpty());
            }

            @Test
            void Then_ParsesNestedChildren() throws IOException {
                var json = """
                    [
                      {
                        "type": "program",
                        "text": "class Foo { void bar() {} }",
                        "startLine": 1,
                        "endLine": 1,
                        "startColumn": 0,
                        "endColumn": 27,
                        "children": [
                          {
                            "type": "class_declaration",
                            "text": "class Foo",
                            "startLine": 1,
                            "endLine": 1,
                            "startColumn": 0,
                            "endColumn": 9,
                            "children": [
                              {
                                "type": "method_declaration",
                                "text": "void bar()",
                                "startLine": 1,
                                "endLine": 1,
                                "startColumn": 12,
                                "endColumn": 22,
                                "children": []
                              }
                            ]
                          }
                        ]
                      }
                    ]
                    """;
                var nodes = TreeSitterJsonMapper.parse(json);
                assertEquals(1, nodes.size());
                assertEquals("program", nodes.get(0).type());
                assertEquals(1, nodes.get(0).children().size());

                var classNode = nodes.get(0).children().get(0);
                assertEquals("class_declaration", classNode.type());
                assertEquals(1, classNode.children().size());

                var methodNode = classNode.children().get(0);
                assertEquals("method_declaration", methodNode.type());
                assertTrue(methodNode.children().isEmpty());
            }

            @Test
            void Then_ParsesMultipleRootNodes() throws IOException {
                var json = """
                    [
                      {"type": "import_declaration", "text": "import foo", "startLine": 1, "endLine": 1, "startColumn": 0, "endColumn": 10, "children": []},
                      {"type": "class_declaration", "text": "class Bar", "startLine": 3, "endLine": 20, "startColumn": 0, "endColumn": 1, "children": []}
                    ]
                    """;
                var nodes = TreeSitterJsonMapper.parse(json);
                assertEquals(2, nodes.size());
                assertEquals("import_declaration", nodes.get(0).type());
                assertEquals("class_declaration", nodes.get(1).type());
            }
        }
    }

    @Nested
    class Given_EmptyInput {

        @Nested
        class When_Parsing {

            @Test
            void Then_NullReturnsEmpty() throws IOException {
                var nodes = TreeSitterJsonMapper.parse(null);
                assertTrue(nodes.isEmpty());
            }

            @Test
            void Then_BlankReturnsEmpty() throws IOException {
                var nodes = TreeSitterJsonMapper.parse("   ");
                assertTrue(nodes.isEmpty());
            }

            @Test
            void Then_EmptyArrayReturnsEmpty() throws IOException {
                var nodes = TreeSitterJsonMapper.parse("[]");
                assertTrue(nodes.isEmpty());
            }
        }
    }

    @Nested
    class Given_ErrorObject {

        @Nested
        class When_Parsing {

            @Test
            void Then_ThrowsIOException() {
                var json = """
                    {"error": "Unsupported language: brainfuck"}
                    """;
                var ex = assertThrows(IOException.class,
                        () -> TreeSitterJsonMapper.parse(json));
                assertTrue(ex.getMessage().contains("brainfuck"));
            }
        }
    }

    @Nested
    class Given_InvalidJson {

        @Nested
        class When_Parsing {

            @Test
            void Then_NonArrayThrowsIOException() {
                var json = "\"just a string\"";
                assertThrows(IOException.class,
                        () -> TreeSitterJsonMapper.parse(json));
            }

            @Test
            void Then_MalformedJsonThrowsIOException() {
                assertThrows(IOException.class,
                        () -> TreeSitterJsonMapper.parse("{broken"));
            }
        }
    }

    @Nested
    class Given_MissingFields {

        @Nested
        class When_Parsing {

            @Test
            void Then_UsesDefaults() throws IOException {
                var json = """
                    [{"type": "identifier"}]
                    """;
                var nodes = TreeSitterJsonMapper.parse(json);
                assertEquals(1, nodes.size());
                var node = nodes.get(0);
                assertEquals("identifier", node.type());
                assertEquals("", node.text());
                assertEquals(1, node.startLine());
                assertEquals(1, node.endLine());
                assertEquals(0, node.startColumn());
                assertEquals(0, node.endColumn());
                assertTrue(node.children().isEmpty());
            }

            @Test
            void Then_NonArrayChildrenAreIgnored() throws IOException {
                var json = """
                    [{"type": "identifier", "children": "not_an_array"}]
                    """;
                var nodes = TreeSitterJsonMapper.parse(json);
                assertEquals(1, nodes.size());
                assertTrue(nodes.get(0).children().isEmpty());
            }
        }
    }

    @Nested
    class Given_DeeplyNestedJson {

        @Nested
        class When_Parsing {

            @Test
            void Then_ThrowsOnExcessiveDepth() {
                // Build JSON with nesting depth > 200
                var sb = new StringBuilder("[");
                for (int i = 0; i < 210; i++) {
                    sb.append("{\"type\":\"n").append(i).append("\",\"children\":[");
                }
                sb.append("{\"type\":\"leaf\",\"children\":[]}");
                for (int i = 0; i < 210; i++) {
                    sb.append("]}");
                }
                sb.append("]");

                assertThrows(IOException.class,
                        () -> TreeSitterJsonMapper.parse(sb.toString()),
                        "Should throw on nesting depth exceeding 200");
            }
        }
    }
}
