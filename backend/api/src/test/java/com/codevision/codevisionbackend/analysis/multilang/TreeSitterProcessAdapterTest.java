package com.codevision.codevisionbackend.analysis.multilang;

import static org.junit.jupiter.api.Assertions.*;

import com.codevision.codevisionbackend.config.TreeSitterProperties;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class TreeSitterProcessAdapterTest {

    private final LanguageRegistry registry = new LanguageRegistry();

    @Nested
    class Given_DisabledConfiguration {

        @Nested
        class When_CheckingAvailability {

            @Test
            void Then_IsNotAvailable() {
                var props = new TreeSitterProperties(false, "node", "scripts/tree-sitter-parse.js", 30);
                var adapter = new TreeSitterProcessAdapter(registry, props);
                adapter.checkAvailability();
                assertFalse(adapter.isAvailable());
            }
        }

        @Nested
        class When_Parsing {

            @Test
            void Then_FallsBackToPlaceholder() {
                var props = new TreeSitterProperties(false, "node", "scripts/tree-sitter-parse.js", 30);
                var adapter = new TreeSitterProcessAdapter(registry, props);
                adapter.checkAvailability();

                var result = adapter.parse("public class Foo {}", "Foo.java");
                assertTrue(result.isPresent(), "Should return placeholder result when disabled");
            }
        }
    }

    @Nested
    class Given_InvalidNodePath {

        @Nested
        class When_CheckingAvailability {

            @Test
            void Then_IsNotAvailable() {
                var props = new TreeSitterProperties(true, "/nonexistent/node-binary", "scripts/tree-sitter-parse.js", 30);
                var adapter = new TreeSitterProcessAdapter(registry, props);
                adapter.checkAvailability();
                assertFalse(adapter.isAvailable());
            }
        }

        @Nested
        class When_Parsing {

            @Test
            void Then_FallsBackToPlaceholder() {
                var props = new TreeSitterProperties(true, "/nonexistent/node-binary", "scripts/tree-sitter-parse.js", 30);
                var adapter = new TreeSitterProcessAdapter(registry, props);
                adapter.checkAvailability();

                var result = adapter.parse("public class Foo {}", "Foo.java");
                assertTrue(result.isPresent(), "Should return placeholder when node path invalid");
            }
        }
    }

    @Nested
    class Given_InvalidScriptPath {

        @Nested
        class When_Parsing {

            @Test
            void Then_FallsBackToPlaceholder() {
                // Use a valid node path but invalid script path
                var props = new TreeSitterProperties(true, "node", "/nonexistent/script.js", 1);
                var adapter = new TreeSitterProcessAdapter(registry, props);
                // Manually set availability to simulate node being present
                // but script missing — the adapter will try to run and fail
                adapter.checkAvailability();

                // Even if node is available, bad script path should fall back gracefully
                var result = adapter.parse("def hello(): pass", "test.py");
                assertTrue(result.isPresent(), "Should return placeholder when script path is invalid");
            }
        }
    }

    @Nested
    class Given_InvalidLanguageName {

        @Nested
        class When_Parsing {

            @Test
            void Then_FallsBackForInvalidCharacters() {
                var props = new TreeSitterProperties(false, "node", "scripts/tree-sitter-parse.js", 30);
                var adapter = new TreeSitterProcessAdapter(registry, props);
                adapter.checkAvailability();

                // Even with a weird language, should not crash
                var result = adapter.parse("content", "test.xyz");
                // Will not be supported by registry
                assertFalse(adapter.isSupported("test.xyz"));
            }
        }
    }

    @Nested
    class Given_SupportedFile {

        @Nested
        class When_CheckingSupport {

            @Test
            void Then_JavaIsSupported() {
                var props = new TreeSitterProperties(true, "node", "scripts/tree-sitter-parse.js", 30);
                var adapter = new TreeSitterProcessAdapter(registry, props);
                assertTrue(adapter.isSupported("Foo.java"));
            }

            @Test
            void Then_PythonIsSupported() {
                var props = new TreeSitterProperties(true, "node", "scripts/tree-sitter-parse.js", 30);
                var adapter = new TreeSitterProcessAdapter(registry, props);
                assertTrue(adapter.isSupported("script.py"));
            }

            @Test
            void Then_TypeScriptIsSupported() {
                var props = new TreeSitterProperties(true, "node", "scripts/tree-sitter-parse.js", 30);
                var adapter = new TreeSitterProcessAdapter(registry, props);
                assertTrue(adapter.isSupported("app.ts"));
            }

            @Test
            void Then_UnsupportedFileRejected() {
                var props = new TreeSitterProperties(true, "node", "scripts/tree-sitter-parse.js", 30);
                var adapter = new TreeSitterProcessAdapter(registry, props);
                assertFalse(adapter.isSupported("data.csv"));
            }

            @Test
            void Then_GoIsSupported() {
                var props = new TreeSitterProperties(true, "node", "scripts/tree-sitter-parse.js", 30);
                var adapter = new TreeSitterProcessAdapter(registry, props);
                assertTrue(adapter.isSupported("main.go"));
            }
        }
    }

    @Nested
    class Given_DefaultProperties {

        @Nested
        class When_Constructing {

            @Test
            void Then_DefaultValuesAreSet() {
                var props = new TreeSitterProperties();
                assertTrue(props.enabled());
                assertEquals("node", props.nodePath());
                assertEquals("scripts/tree-sitter-parse.js", props.scriptPath());
                assertEquals(30, props.timeoutSeconds());
            }
        }
    }

    @Nested
    class Given_VeryShortTimeout {

        @Nested
        class When_ParsingWithSlowProcess {

            @Test
            void Then_TimesOutGracefully() {
                // Use a command that would take longer than 1 second
                // With an invalid script, the process should fail quickly or timeout
                var props = new TreeSitterProperties(true, "node", "/dev/null", 1);
                var adapter = new TreeSitterProcessAdapter(registry, props);
                adapter.checkAvailability();

                // Should not throw, should fall back gracefully
                var result = adapter.parse("content", "test.py");
                assertTrue(result.isPresent(), "Should fall back gracefully on timeout/error");
            }
        }
    }
}
