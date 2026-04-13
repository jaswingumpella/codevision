package com.codevision.codevisionbackend.analysis.multilang;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TreeSitterBridgeTest {

    private final LanguageRegistry registry = new LanguageRegistry();
    private final TreeSitterBridge bridge = new TreeSitterBridge(registry);

    @Nested
    class Given_JavaFilename {

        @Nested
        class When_CheckingIsSupported {

            @Test
            void Then_ReturnsTrue() {
                assertThat(bridge.isSupported("MyService.java")).isTrue();
            }
        }
    }

    @Nested
    class Given_UnknownXyzFilename {

        @Nested
        class When_CheckingIsSupported {

            @Test
            void Then_ReturnsFalse() {
                assertThat(bridge.isSupported("unknown.xyz")).isFalse();
            }
        }
    }

    @Nested
    class Given_ValidSourceAndJavaFilename {

        @Nested
        class When_Parsing {

            @Test
            void Then_ReturnsParsedTree() {
                var source = "public class Hello {}";
                var result = bridge.parse(source, "Hello.java");

                assertThat(result).isPresent();
                var tree = result.get();
                assertThat(tree.sourceFile()).isEqualTo("Hello.java");
                assertThat(tree.languageName()).isEqualTo("java");
                assertThat(tree.sourceCode()).isEqualTo(source);
                assertThat(tree.rootNodes()).isNotEmpty();
            }
        }
    }

    @Nested
    class Given_UnsupportedFilename {

        @Nested
        class When_Parsing {

            @Test
            void Then_ReturnsEmptyOptional() {
                var result = bridge.parse("some content", "data.xyz");

                assertThat(result).isEmpty();
            }
        }
    }

    @Nested
    class Given_EmptySource {

        @Nested
        class When_Parsing {

            @Test
            void Then_ReturnsParsedTreeWithRootNode() {
                var result = bridge.parse("", "Empty.java");

                assertThat(result).isPresent();
                var tree = result.get();
                assertThat(tree.rootNodes()).hasSize(1);
                assertThat(tree.rootNodes().get(0).type()).isEqualTo("source_file");
            }
        }
    }
}
