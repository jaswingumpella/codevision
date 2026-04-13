package com.codevision.codevisionbackend.dependency;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GradleDependencyResolverTest {

    private final GradleDependencyResolver resolver = new GradleDependencyResolver();

    @Nested
    class Given_DirectoryWithBuildGradle {

        @Nested
        class When_CheckingSupport {

            @Test
            void Then_ReturnsTrue(@TempDir Path tempDir) throws Exception {
                Files.writeString(tempDir.resolve("build.gradle"), "apply plugin: 'java'");
                assertTrue(resolver.supports(tempDir));
            }
        }
    }

    @Nested
    class Given_DirectoryWithBuildGradleKts {

        @Nested
        class When_CheckingSupport {

            @Test
            void Then_ReturnsTrue(@TempDir Path tempDir) throws Exception {
                Files.writeString(tempDir.resolve("build.gradle.kts"), "plugins { java }");
                assertTrue(resolver.supports(tempDir));
            }
        }
    }

    @Nested
    class Given_DirectoryWithoutGradleFiles {

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
            void Then_ReturnsGradle() {
                assertEquals("gradle", resolver.buildSystem());
            }
        }
    }

    @Nested
    class Given_GradleTreeOutput {

        @Nested
        class When_Parsing {

            @Test
            void Then_BuildsCorrectTree() {
                var lines = List.of(
                        "com.example:my-app:1.0.0",
                        "+--- org.springframework.boot:spring-boot-starter-web:3.2.5",
                        "|    +--- org.springframework.boot:spring-boot-starter:3.2.5",
                        "\\--- com.google.guava:guava:32.1.3-jre"
                );
                var tree = resolver.parseTreeOutput(lines, ExclusionConfig.none());

                assertNotNull(tree);
                assertEquals("com.example", tree.artifact().groupId());
                assertEquals("my-app", tree.artifact().artifactId());
                assertEquals("1.0.0", tree.artifact().version());

                assertEquals(2, tree.children().size());

                var starterWeb = tree.children().get(0);
                assertEquals("org.springframework.boot", starterWeb.artifact().groupId());
                assertEquals("spring-boot-starter-web", starterWeb.artifact().artifactId());
                assertEquals("3.2.5", starterWeb.artifact().version());

                assertEquals(1, starterWeb.children().size());
                var starter = starterWeb.children().get(0);
                assertEquals("spring-boot-starter", starter.artifact().artifactId());

                var guava = tree.children().get(1);
                assertEquals("com.google.guava", guava.artifact().groupId());
                assertEquals("guava", guava.artifact().artifactId());
                assertEquals("32.1.3-jre", guava.artifact().version());
            }

            @Test
            void Then_HandlesVersionConstraintAnnotations() {
                var lines = List.of(
                        "com.example:my-app:1.0.0",
                        "+--- org.apache.commons:commons-lang3:3.14.0",
                        "\\--- com.google.guava:guava:32.1.3-jre -> 33.0.0-jre"
                );
                var tree = resolver.parseTreeOutput(lines, ExclusionConfig.none());

                assertEquals(3, tree.totalArtifactCount());
                var guava = tree.children().get(1);
                assertEquals("33.0.0-jre", guava.artifact().version());
            }

            @Test
            void Then_HandlesStarredDependencies() {
                var lines = List.of(
                        "com.example:my-app:1.0.0",
                        "+--- org.apache.commons:commons-lang3:3.14.0",
                        "\\--- com.google.guava:guava:33.0.0-jre (*)"
                );
                var tree = resolver.parseTreeOutput(lines, ExclusionConfig.none());

                assertEquals(3, tree.totalArtifactCount());
                var guava = tree.children().get(1);
                assertEquals("guava", guava.artifact().artifactId());
                assertEquals("33.0.0-jre", guava.artifact().version());
            }

            @Test
            void Then_AppliesExclusions() {
                var lines = List.of(
                        "com.example:my-app:1.0.0",
                        "+--- org.springframework:spring-core:6.1.0",
                        "+--- com.mycompany:my-lib:2.0.0",
                        "\\--- org.slf4j:slf4j-api:2.0.11"
                );
                var tree = resolver.parseTreeOutput(lines, ExclusionConfig.defaults());
                // Spring and SLF4J excluded, only root + my-lib remain
                assertEquals(2, tree.totalArtifactCount());
                assertEquals(1, tree.children().size());
                assertEquals("my-lib", tree.children().get(0).artifact().artifactId());
            }

            @Test
            void Then_DeduplicatesByCoordinates() {
                var lines = List.of(
                        "com.example:my-app:1.0.0",
                        "+--- com.google.guava:guava:33.0.0-jre",
                        "\\--- com.google.guava:guava:33.0.0-jre"
                );
                var tree = resolver.parseTreeOutput(lines, ExclusionConfig.none());
                assertEquals(2, tree.totalArtifactCount());
                assertEquals(1, tree.children().size());
            }

            @Test
            void Then_SkipsMalformedAndBlankLines() {
                var lines = List.of(
                        "com.example:my-app:1.0.0",
                        "",
                        "this is garbage",
                        "+--- com.valid:lib-a:1.0.0",
                        "\\--- com.valid:lib-b:2.0.0"
                );
                var tree = resolver.parseTreeOutput(lines, ExclusionConfig.none());
                assertEquals(3, tree.totalArtifactCount());
                assertEquals(2, tree.children().size());
            }

            @Test
            void Then_HandlesNullExclusionConfig() {
                var lines = List.of(
                        "com.example:my-app:1.0.0",
                        "+--- org.springframework:spring-core:6.1.0",
                        "\\--- org.slf4j:slf4j-api:2.0.11"
                );
                var tree = resolver.parseTreeOutput(lines, null);
                assertEquals(3, tree.totalArtifactCount());
            }

            @Test
            void Then_HandlesDeeplyNestedTree() {
                var lines = List.of(
                        "com.example:my-app:1.0.0",
                        "+--- com.a:lib-a:1.0",
                        "|    +--- com.b:lib-b:2.0",
                        "|    |    \\--- com.c:lib-c:3.0",
                        "\\--- com.d:lib-d:4.0"
                );
                var tree = resolver.parseTreeOutput(lines, ExclusionConfig.none());
                assertEquals(5, tree.totalArtifactCount());
                assertEquals(3, tree.maxDepth());

                var libA = tree.children().get(0);
                assertEquals("lib-a", libA.artifact().artifactId());
                var libB = libA.children().get(0);
                assertEquals("lib-b", libB.artifact().artifactId());
                var libC = libB.children().get(0);
                assertEquals("lib-c", libC.artifact().artifactId());
            }
        }
    }

    @Nested
    class Given_EmptyOutput {

        @Nested
        class When_Parsing {

            @Test
            void Then_ReturnsUnknownRoot() {
                var tree = resolver.parseTreeOutput(List.of(), ExclusionConfig.none());
                assertNotNull(tree);
                assertEquals("unknown", tree.artifact().groupId());
                assertEquals("unknown", tree.artifact().artifactId());
                assertEquals("0.0.0", tree.artifact().version());
            }
        }
    }
}
