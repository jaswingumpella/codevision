package com.codevision.codevisionbackend.dependency;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class NpmDependencyResolverTest {

    private final NpmDependencyResolver resolver = new NpmDependencyResolver();

    @Nested
    class Given_DirectoryWithPackageJson {

        @Nested
        class When_CheckingSupport {

            @Test
            void Then_ReturnsTrue(@TempDir Path tempDir) throws Exception {
                Files.writeString(tempDir.resolve("package.json"), "{}");
                assertTrue(resolver.supports(tempDir));
            }
        }
    }

    @Nested
    class Given_DirectoryWithoutPackageJson {

        @Nested
        class When_CheckingSupport {

            @Test
            void Then_ReturnsFalse(@TempDir Path tempDir) {
                assertFalse(resolver.supports(tempDir));
            }
        }
    }

    @Nested
    class Given_Metadata {

        @Nested
        class When_QueryingBuildSystem {

            @Test
            void Then_ReturnsNpm() {
                assertEquals("npm", resolver.buildSystem());
            }
        }
    }

    @Nested
    class Given_NpmLsJsonOutput {

        @Nested
        class When_Parsing {

            @Test
            void Then_BuildsCorrectTree() {
                var json = """
                        {
                          "name": "my-app",
                          "version": "1.0.0",
                          "dependencies": {
                            "react": {
                              "version": "18.2.0",
                              "dependencies": {
                                "loose-envify": {
                                  "version": "1.4.0"
                                }
                              }
                            },
                            "express": {
                              "version": "4.18.2"
                            }
                          }
                        }
                        """;
                var tree = resolver.parseJsonOutput(json, ExclusionConfig.none());

                assertNotNull(tree);
                assertEquals("my-app", tree.artifact().groupId());
                assertEquals("my-app", tree.artifact().artifactId());
                assertEquals("1.0.0", tree.artifact().version());

                assertEquals(2, tree.children().size());

                var react = tree.children().stream()
                        .filter(c -> c.artifact().artifactId().equals("react"))
                        .findFirst().orElseThrow();
                assertEquals("react", react.artifact().groupId());
                assertEquals("react", react.artifact().artifactId());
                assertEquals("18.2.0", react.artifact().version());
                assertEquals(1, react.children().size());

                var looseEnvify = react.children().get(0);
                assertEquals("loose-envify", looseEnvify.artifact().artifactId());
                assertEquals("1.4.0", looseEnvify.artifact().version());

                var express = tree.children().stream()
                        .filter(c -> c.artifact().artifactId().equals("express"))
                        .findFirst().orElseThrow();
                assertEquals("express", express.artifact().artifactId());
                assertEquals("4.18.2", express.artifact().version());
                assertEquals(0, express.children().size());
            }

            @Test
            void Then_HandlesScopedPackages() {
                var json = """
                        {
                          "name": "my-app",
                          "version": "1.0.0",
                          "dependencies": {
                            "@types/react": {
                              "version": "18.2.45"
                            }
                          }
                        }
                        """;
                var tree = resolver.parseJsonOutput(json, ExclusionConfig.none());

                assertEquals(1, tree.children().size());
                var typesReact = tree.children().get(0);
                assertEquals("@types", typesReact.artifact().groupId());
                assertEquals("react", typesReact.artifact().artifactId());
                assertEquals("18.2.45", typesReact.artifact().version());
            }

            @Test
            void Then_AppliesExclusionConfig() {
                var json = """
                        {
                          "name": "my-app",
                          "version": "1.0.0",
                          "dependencies": {
                            "react": {
                              "version": "18.2.0"
                            },
                            "excluded-lib": {
                              "version": "1.0.0"
                            }
                          }
                        }
                        """;
                var exclusion = new ExclusionConfig(java.util.List.of("excluded-lib:excluded-lib"));
                var tree = resolver.parseJsonOutput(json, exclusion);

                assertEquals(1, tree.children().size());
                assertEquals("react", tree.children().get(0).artifact().artifactId());
            }

            @Test
            void Then_DeduplicatesCyclicDependencies() {
                var json = """
                        {
                          "name": "my-app",
                          "version": "1.0.0",
                          "dependencies": {
                            "a": {
                              "version": "1.0.0",
                              "dependencies": {
                                "b": {
                                  "version": "2.0.0",
                                  "dependencies": {
                                    "a": {
                                      "version": "1.0.0"
                                    }
                                  }
                                }
                              }
                            }
                          }
                        }
                        """;
                var tree = resolver.parseJsonOutput(json, ExclusionConfig.none());

                // root (my-app) + a + b = 3, second occurrence of 'a' is deduplicated
                assertEquals(3, tree.totalArtifactCount());
            }
        }
    }

    @Nested
    class Given_EmptyJson {

        @Nested
        class When_Parsing {

            @Test
            void Then_ReturnsUnknownRoot() {
                var tree = resolver.parseJsonOutput("{}", ExclusionConfig.none());
                assertNotNull(tree);
                assertEquals("unknown", tree.artifact().groupId());
                assertEquals("unknown", tree.artifact().artifactId());
                assertEquals("0.0.0", tree.artifact().version());
            }

            @Test
            void Then_HandlesNullJsonGracefully() {
                var tree = resolver.parseJsonOutput(null, ExclusionConfig.none());
                assertNotNull(tree);
                assertEquals("unknown", tree.artifact().groupId());
            }

            @Test
            void Then_HandlesMalformedJsonGracefully() {
                var tree = resolver.parseJsonOutput("not valid json", ExclusionConfig.none());
                assertNotNull(tree);
                assertEquals("unknown", tree.artifact().groupId());
            }
        }
    }
}
