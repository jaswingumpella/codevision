package com.codevision.codevisionbackend.analysis.multilang;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LanguageRegistryTest {

    private final LanguageRegistry registry = new LanguageRegistry();

    @Nested
    class Given_RegisteredLanguages {

        @Nested
        class When_DetectingByJavaExtension {

            @Test
            void Then_ReturnsJavaDefinition() {
                var result = registry.detect("MyClass.java");

                assertThat(result).isPresent();
                assertThat(result.get().name()).isEqualTo("java");
                assertThat(result.get().displayName()).isEqualTo("Java");
                assertThat(result.get().treeSitterLanguage()).isEqualTo("java");
            }
        }

        @Nested
        class When_DetectingByTypeScriptExtension {

            @Test
            void Then_ReturnsTypeScriptDefinition() {
                var result = registry.detect("app.ts");

                assertThat(result).isPresent();
                assertThat(result.get().name()).isEqualTo("typescript");
            }

            @Test
            void Then_RecognizesTsxExtension() {
                var result = registry.detect("Component.tsx");

                assertThat(result).isPresent();
                assertThat(result.get().name()).isEqualTo("typescript");
            }
        }

        @Nested
        class When_DetectingByPythonExtension {

            @Test
            void Then_ReturnsPythonDefinition() {
                var result = registry.detect("main.py");

                assertThat(result).isPresent();
                assertThat(result.get().name()).isEqualTo("python");
            }
        }

        @Nested
        class When_DetectingByGoExtension {

            @Test
            void Then_ReturnsGoDefinition() {
                var result = registry.detect("main.go");

                assertThat(result).isPresent();
                assertThat(result.get().name()).isEqualTo("go");
            }
        }

        @Nested
        class When_DetectingByRustExtension {

            @Test
            void Then_ReturnsRustDefinition() {
                var result = registry.detect("lib.rs");

                assertThat(result).isPresent();
                assertThat(result.get().name()).isEqualTo("rust");
            }
        }

        @Nested
        class When_DetectingByKotlinExtension {

            @Test
            void Then_ReturnsKotlinDefinition() {
                var result = registry.detect("App.kt");

                assertThat(result).isPresent();
                assertThat(result.get().name()).isEqualTo("kotlin");
            }
        }

        @Nested
        class When_DetectingUnknownExtension {

            @Test
            void Then_ReturnsEmpty() {
                var result = registry.detect("file.xyz");

                assertThat(result).isEmpty();
            }
        }

        @Nested
        class When_DetectingNullFilename {

            @Test
            void Then_ReturnsEmpty() {
                var result = registry.detect(null);

                assertThat(result).isEmpty();
            }
        }

        @Nested
        class When_DetectingFileWithNoExtension {

            @Test
            void Then_ReturnsEmptyForUnrecognized() {
                var result = registry.detect("README");

                assertThat(result).isEmpty();
            }

            @Test
            void Then_RecognizesDockerfile() {
                var result = registry.detect("Dockerfile");

                assertThat(result).isPresent();
                assertThat(result.get().name()).isEqualTo("dockerfile");
            }

            @Test
            void Then_RecognizesMakefile() {
                var result = registry.detect("Makefile");

                assertThat(result).isPresent();
                assertThat(result.get().name()).isEqualTo("makefile");
            }
        }

        @Nested
        class When_DetectingWithFullPath {

            @Test
            void Then_ExtractsExtensionFromPath() {
                var result = registry.detect("/home/user/project/src/Main.java");

                assertThat(result).isPresent();
                assertThat(result.get().name()).isEqualTo("java");
            }
        }
    }

    @Nested
    class Given_AllRegisteredLanguages {

        @Nested
        class When_ListingAllLanguages {

            @Test
            void Then_Returns50PlusEntries() {
                var all = registry.allLanguages();

                assertThat(all).hasSizeGreaterThanOrEqualTo(50);
            }
        }

        @Nested
        class When_CheckingSupportedExtensions {

            @Test
            void Then_IncludesCommonExtensions() {
                var extensions = registry.supportedExtensions();

                assertThat(extensions).contains(
                        ".java", ".py", ".ts", ".tsx", ".js", ".go",
                        ".rs", ".kt", ".rb", ".swift", ".cpp", ".cs",
                        ".scala", ".dart", ".lua", ".php"
                );
            }
        }

        @Nested
        class When_LookingUpByName {

            @Test
            void Then_ReturnsCorrectDefinition() {
                var result = registry.getLanguage("python");

                assertThat(result).isPresent();
                assertThat(result.get().displayName()).isEqualTo("Python");
                assertThat(result.get().fileExtensions()).contains(".py");
            }

            @Test
            void Then_ReturnsEmptyForUnknownName() {
                var result = registry.getLanguage("nonexistent");

                assertThat(result).isEmpty();
            }
        }
    }
}
