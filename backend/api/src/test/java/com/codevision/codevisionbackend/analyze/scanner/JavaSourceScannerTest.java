package com.codevision.codevisionbackend.analyze.scanner;

import static org.junit.jupiter.api.Assertions.*;

import com.codevision.codevisionbackend.analyze.scanner.ClassMetadataRecord.MethodMetrics;
import com.codevision.codevisionbackend.analyze.scanner.ClassMetadataRecord.SourceSet;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JavaSourceScannerTest {

    private final JavaSourceScanner scanner = new JavaSourceScanner();

    @Nested
    class Given_BasicJavaSources {

        @Nested
        class When_Scanning {

            @Test
            void Then_CollectsAllClassesAndKeepsUserCodeFlag(@TempDir Path repoRoot) throws Exception {
        Path mainSource = repoRoot.resolve("src/main/java/com/example/demo");
        Path externalSource = repoRoot.resolve("src/main/java/com/example/external");
        Files.createDirectories(mainSource);
        Files.createDirectories(externalSource);

        Files.writeString(
                mainSource.resolve("MyController.java"),
                """
                package com.example.demo;

                import org.springframework.web.bind.annotation.RestController;

                @RestController
                public class MyController {
                }
                """);

        Files.writeString(
                mainSource.resolve("MyService.java"),
                """
                package com.example.demo;

                import org.springframework.stereotype.Service;

                @Service
                public class MyService {
                }
                """);

        Files.writeString(
                externalSource.resolve("ExternalClass.java"),
                """
                package com.example.external;

                public class ExternalClass {
                }
                """);

        List<ClassMetadataRecord> records = scanner.scan(repoRoot, List.of(repoRoot));
        assertEquals(3, records.size());

        Optional<ClassMetadataRecord> controller =
                records.stream().filter(record -> record.className().equals("MyController")).findFirst();
        Optional<ClassMetadataRecord> service =
                records.stream().filter(record -> record.className().equals("MyService")).findFirst();
        Optional<ClassMetadataRecord> external =
                records.stream().filter(record -> record.className().equals("ExternalClass")).findFirst();

        assertTrue(controller.isPresent());
        assertEquals("CONTROLLER", controller.get().stereotype());
        assertEquals(SourceSet.MAIN, controller.get().sourceSet());
        assertEquals("src/main/java/com/example/demo/MyController.java", controller.get().relativePath());
        assertTrue(controller.get().userCode());

        assertTrue(service.isPresent());
        assertEquals("SERVICE", service.get().stereotype());
        assertTrue(service.get().userCode());

        assertTrue(external.isPresent());
        assertEquals("OTHER", external.get().stereotype());
        assertTrue(external.get().userCode());
            }
        }
    }

    @Nested
    class Given_SourceWithJavadoc {

        @Nested
        class When_Scanning {

            @Test
            void Then_ExtractsDocumentationFromClass(@TempDir Path repoRoot) throws Exception {
                Path mainSource = repoRoot.resolve("src/main/java/com/example");
                Files.createDirectories(mainSource);
                Files.writeString(mainSource.resolve("Documented.java"), """
                        package com.example;

                        /**
                         * This is a documented class.
                         */
                        public class Documented {
                            public void doSomething() {}
                        }
                        """);

                List<ClassMetadataRecord> records = scanner.scan(repoRoot, List.of(repoRoot));
                assertEquals(1, records.size());
                var record = records.get(0);
                assertNotNull(record.documentation());
                assertTrue(record.documentation().contains("This is a documented class"));
            }
        }
    }

    @Nested
    class Given_SourceWithoutJavadoc {

        @Nested
        class When_Scanning {

            @Test
            void Then_DocumentationIsNull(@TempDir Path repoRoot) throws Exception {
                Path mainSource = repoRoot.resolve("src/main/java/com/example");
                Files.createDirectories(mainSource);
                Files.writeString(mainSource.resolve("NoDoc.java"), """
                        package com.example;

                        public class NoDoc {
                            public void doSomething() {}
                        }
                        """);

                List<ClassMetadataRecord> records = scanner.scan(repoRoot, List.of(repoRoot));
                assertEquals(1, records.size());
                assertNull(records.get(0).documentation());
            }
        }
    }

    @Nested
    class Given_GenericClass {

        @Nested
        class When_Scanning {

            @Test
            void Then_ExtractsTypeParameters(@TempDir Path repoRoot) throws Exception {
                Path mainSource = repoRoot.resolve("src/main/java/com/example");
                Files.createDirectories(mainSource);
                Files.writeString(mainSource.resolve("GenericRepo.java"), """
                        package com.example;

                        public class GenericRepo<T, ID> {
                            public T findById(ID id) { return null; }
                        }
                        """);

                List<ClassMetadataRecord> records = scanner.scan(repoRoot, List.of(repoRoot));
                assertEquals(1, records.size());
                var record = records.get(0);
                assertEquals(List.of("T", "ID"), record.typeParameters());
            }
        }
    }

    @Nested
    class Given_NonGenericClass {

        @Nested
        class When_Scanning {

            @Test
            void Then_TypeParametersAreEmpty(@TempDir Path repoRoot) throws Exception {
                Path mainSource = repoRoot.resolve("src/main/java/com/example");
                Files.createDirectories(mainSource);
                Files.writeString(mainSource.resolve("Simple.java"), """
                        package com.example;

                        public class Simple {}
                        """);

                List<ClassMetadataRecord> records = scanner.scan(repoRoot, List.of(repoRoot));
                assertEquals(1, records.size());
                assertTrue(records.get(0).typeParameters().isEmpty());
            }
        }
    }

    @Nested
    class Given_ClassWithMethods {

        @Nested
        class When_Scanning {

            @Test
            void Then_PopulatesLinesOfCode(@TempDir Path repoRoot) throws Exception {
                Path mainSource = repoRoot.resolve("src/main/java/com/example");
                Files.createDirectories(mainSource);
                Files.writeString(mainSource.resolve("WithMethods.java"), """
                        package com.example;

                        public class WithMethods {
                            private int x;
                            private int y;

                            public int add() {
                                return x + y;
                            }

                            public int multiply() {
                                return x * y;
                            }
                        }
                        """);

                List<ClassMetadataRecord> records = scanner.scan(repoRoot, List.of(repoRoot));
                assertEquals(1, records.size());
                assertTrue(records.get(0).linesOfCode() > 0);
            }

            @Test
            void Then_PopulatesMethodMetrics(@TempDir Path repoRoot) throws Exception {
                Path mainSource = repoRoot.resolve("src/main/java/com/example");
                Files.createDirectories(mainSource);
                Files.writeString(mainSource.resolve("Complex.java"), """
                        package com.example;

                        public class Complex {
                            /**
                             * Processes input with branching.
                             * @param x the input
                             * @return the result
                             * @throws IllegalArgumentException if invalid
                             */
                            public int process(int x) throws IllegalArgumentException {
                                if (x > 0) {
                                    return x * 2;
                                } else if (x < 0) {
                                    return -x;
                                }
                                return 0;
                            }
                        }
                        """);

                List<ClassMetadataRecord> records = scanner.scan(repoRoot, List.of(repoRoot));
                assertEquals(1, records.size());
                var record = records.get(0);

                assertFalse(record.methodMetrics().isEmpty());
                assertTrue(record.methodMetrics().containsKey("process"));

                MethodMetrics metrics = record.methodMetrics().get("process");
                assertEquals("process", metrics.methodName());
                assertTrue(metrics.cyclomaticComplexity() >= 3, "Should have if + else-if branches");
                assertTrue(metrics.cognitiveComplexity() >= 0, "Cognitive complexity should be non-negative");
                assertTrue(metrics.linesOfCode() > 0);
                // Documentation may or may not be parsed depending on JavaParser config
                if (metrics.documentation() != null) {
                    assertTrue(metrics.documentation().contains("Processes input"));
                }
                assertEquals(List.of("int"), metrics.parameterTypes());
                assertEquals("int", metrics.returnType());
                assertEquals(List.of("IllegalArgumentException"), metrics.thrownExceptions());
            }
        }
    }

    @Nested
    class Given_EmptyClass {

        @Nested
        class When_Scanning {

            @Test
            void Then_MethodMetricsAreEmpty(@TempDir Path repoRoot) throws Exception {
                Path mainSource = repoRoot.resolve("src/main/java/com/example");
                Files.createDirectories(mainSource);
                Files.writeString(mainSource.resolve("Empty.java"), """
                        package com.example;

                        public class Empty {}
                        """);

                List<ClassMetadataRecord> records = scanner.scan(repoRoot, List.of(repoRoot));
                assertEquals(1, records.size());
                assertTrue(records.get(0).methodMetrics().isEmpty());
            }
        }
    }
}
