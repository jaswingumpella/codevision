package com.codevision.codevisionbackend.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("NodeMetadata")
class NodeMetadataTest {

    // ── Fixture helpers ─────────────────────────────────────────────────

    private static NodeMetadata fullyPopulated() {
        return new NodeMetadata(
                "public",
                Set.of("static", "final"),
                List.of(
                        new AnnotationValue("Override", "java.lang.Override", Map.of()),
                        new AnnotationValue("Deprecated", "java.lang.Deprecated", Map.of("since", "1.0"))
                ),
                List.of("T", "K extends Comparable"),
                "java.lang.String",
                List.of("int", "java.lang.String"),
                List.of("java.io.IOException"),
                "Processes the input and returns a result.",
                5,
                3,
                42,
                "null",
                "/src/main/java/com/example/Foo.java",
                10,
                52,
                Map.of("framework", "spring", "version", "3.2.5")
        );
    }

    private static NodeMetadata minimal() {
        return new NodeMetadata(
                null, null, null, null, null, null, null, null,
                0, 0, 0, null, null, 0, 0, null
        );
    }

    // ── Tests ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Given fully populated metadata")
    class Given_FullyPopulatedMetadata {

        private final NodeMetadata metadata = fullyPopulated();

        @Nested
        @DisplayName("When accessing fields")
        class When_Accessing {

            @Test
            @DisplayName("Then visibility is present")
            void Then_VisibilityIsPresent() {
                assertEquals("public", metadata.visibility());
            }

            @Test
            @DisplayName("Then modifiers are present")
            void Then_ModifiersArePresent() {
                assertTrue(metadata.modifiers().contains("static"));
                assertTrue(metadata.modifiers().contains("final"));
            }

            @Test
            @DisplayName("Then annotations are present")
            void Then_AnnotationsArePresent() {
                assertEquals(2, metadata.annotations().size());
                assertEquals("Override", metadata.annotations().get(0).name());
            }

            @Test
            @DisplayName("Then type parameters are present")
            void Then_TypeParametersArePresent() {
                assertEquals(2, metadata.typeParameters().size());
                assertEquals("T", metadata.typeParameters().get(0));
            }

            @Test
            @DisplayName("Then return type is present")
            void Then_ReturnTypeIsPresent() {
                assertEquals("java.lang.String", metadata.returnType());
            }

            @Test
            @DisplayName("Then parameter types are present")
            void Then_ParameterTypesArePresent() {
                assertEquals(2, metadata.parameterTypes().size());
            }

            @Test
            @DisplayName("Then thrown exceptions are present")
            void Then_ThrownExceptionsArePresent() {
                assertEquals(1, metadata.thrownExceptions().size());
                assertEquals("java.io.IOException", metadata.thrownExceptions().get(0));
            }

            @Test
            @DisplayName("Then documentation is present")
            void Then_DocumentationIsPresent() {
                assertNotNull(metadata.documentation());
                assertTrue(metadata.documentation().contains("Processes"));
            }

            @Test
            @DisplayName("Then cyclomatic complexity is present")
            void Then_CyclomaticComplexityIsPresent() {
                assertEquals(5, metadata.cyclomaticComplexity());
            }

            @Test
            @DisplayName("Then cognitive complexity is present")
            void Then_CognitiveComplexityIsPresent() {
                assertEquals(3, metadata.cognitiveComplexity());
            }

            @Test
            @DisplayName("Then lines of code is present")
            void Then_LinesOfCodeIsPresent() {
                assertEquals(42, metadata.linesOfCode());
            }

            @Test
            @DisplayName("Then default value is present")
            void Then_DefaultValueIsPresent() {
                assertEquals("null", metadata.defaultValue());
            }

            @Test
            @DisplayName("Then source file is present")
            void Then_SourceFileIsPresent() {
                assertEquals("/src/main/java/com/example/Foo.java", metadata.sourceFile());
            }

            @Test
            @DisplayName("Then start and end lines are present")
            void Then_StartAndEndLinesArePresent() {
                assertEquals(10, metadata.startLine());
                assertEquals(52, metadata.endLine());
            }

            @Test
            @DisplayName("Then language-specific data is present")
            void Then_LanguageSpecificIsPresent() {
                assertEquals("spring", metadata.languageSpecific().get("framework"));
            }
        }
    }

    @Nested
    @DisplayName("Given minimal metadata")
    class Given_MinimalMetadata {

        private final NodeMetadata metadata = minimal();

        @Nested
        @DisplayName("When accessing fields")
        class When_Accessing {

            @Test
            @DisplayName("Then nullable fields are null")
            void Then_NullableFieldsAreNull() {
                assertNull(metadata.visibility());
                assertNull(metadata.modifiers());
                assertNull(metadata.annotations());
                assertNull(metadata.typeParameters());
                assertNull(metadata.returnType());
                assertNull(metadata.parameterTypes());
                assertNull(metadata.thrownExceptions());
                assertNull(metadata.documentation());
                assertNull(metadata.defaultValue());
                assertNull(metadata.sourceFile());
                assertNull(metadata.languageSpecific());
            }

            @Test
            @DisplayName("Then numeric fields default to zero")
            void Then_NumericFieldsDefaultToZero() {
                assertEquals(0, metadata.cyclomaticComplexity());
                assertEquals(0, metadata.cognitiveComplexity());
                assertEquals(0, metadata.linesOfCode());
                assertEquals(0, metadata.startLine());
                assertEquals(0, metadata.endLine());
            }
        }
    }

    @Nested
    @DisplayName("Given an AnnotationValue")
    class Given_AnnotationValue {

        @Test
        @DisplayName("Then all fields are accessible")
        void Then_AllFieldsAccessible() {
            AnnotationValue av = new AnnotationValue(
                    "Service",
                    "org.springframework.stereotype.Service",
                    Map.of("value", "myService")
            );

            assertEquals("Service", av.name());
            assertEquals("org.springframework.stereotype.Service", av.qualifiedName());
            assertEquals("myService", av.parameters().get("value"));
        }
    }

    @Nested
    @DisplayName("Given a Provenance record")
    class Given_Provenance {

        @Test
        @DisplayName("Then all fields are accessible")
        void Then_AllFieldsAccessible() {
            Provenance prov = new Provenance(
                    "BytecodeScanner",
                    "/src/main/java/Foo.java",
                    42,
                    ConfidenceLevel.RESOLVED
            );

            assertEquals("BytecodeScanner", prov.scannerName());
            assertEquals("/src/main/java/Foo.java", prov.sourceFile());
            assertEquals(42, prov.lineNumber());
            assertEquals(ConfidenceLevel.RESOLVED, prov.confidence());
        }
    }
}
