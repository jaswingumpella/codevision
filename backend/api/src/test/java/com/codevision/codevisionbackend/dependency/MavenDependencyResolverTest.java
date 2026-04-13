package com.codevision.codevisionbackend.dependency;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MavenDependencyResolverTest {

    private final MavenDependencyResolver resolver = new MavenDependencyResolver();

    @Nested
    class Given_ProjectWithPomXml {

        @Nested
        class When_CheckingSupport {

            @Test
            void Then_ReturnsTrueForMavenProject(@TempDir Path tempDir) throws Exception {
                Files.writeString(tempDir.resolve("pom.xml"), "<project></project>");
                assertTrue(resolver.supports(tempDir));
            }
        }
    }

    @Nested
    class Given_ProjectWithoutPomXml {

        @Nested
        class When_CheckingSupport {

            @Test
            void Then_ReturnsFalseForNonMavenProject(@TempDir Path tempDir) {
                assertFalse(resolver.supports(tempDir));
            }
        }
    }

    @Nested
    class Given_BuildSystemName {

        @Nested
        class When_Querying {

            @Test
            void Then_ReturnsMaven() {
                assertEquals("maven", resolver.buildSystem());
            }
        }
    }

    @Nested
    class Given_MavenTreeOutput {

        @Nested
        class When_ParsingSimpleTree {

            @Test
            void Then_ParsesRootArtifact() {
                var lines = List.of(
                        "com.example:my-project:jar:1.0.0"
                );
                var tree = resolver.parseTreeOutput(lines, ExclusionConfig.none());
                assertNotNull(tree);
                assertEquals("com.example", tree.artifact().groupId());
                assertEquals("my-project", tree.artifact().artifactId());
                assertEquals("1.0.0", tree.artifact().version());
            }

            @Test
            void Then_ParsesDirectDependencies() {
                var lines = List.of(
                        "com.example:my-project:jar:1.0.0",
                        "+- org.apache.commons:commons-lang3:jar:3.14.0:compile",
                        "+- com.google.guava:guava:jar:33.0.0-jre:compile",
                        "\\- org.slf4j:slf4j-api:jar:2.0.11:compile"
                );
                var tree = resolver.parseTreeOutput(lines, ExclusionConfig.none());
                assertEquals(4, tree.totalArtifactCount());
                assertEquals(3, tree.children().size());
                assertEquals("commons-lang3", tree.children().get(0).artifact().artifactId());
            }
        }

        @Nested
        class When_ParsingTransitiveDependencies {

            @Test
            void Then_ParsesNestedTree() {
                var lines = List.of(
                        "com.example:my-project:jar:1.0.0",
                        "+- com.google.guava:guava:jar:33.0.0-jre:compile",
                        "|  +- com.google.guava:failureaccess:jar:1.0.2:compile",
                        "|  \\- com.google.guava:listenablefuture:jar:9999.0-empty-to-avoid-conflict-with-guava:compile",
                        "\\- org.slf4j:slf4j-api:jar:2.0.11:compile"
                );
                var tree = resolver.parseTreeOutput(lines, ExclusionConfig.none());
                assertEquals(5, tree.totalArtifactCount());
                assertEquals(2, tree.children().size());

                var guava = tree.children().get(0);
                assertEquals("guava", guava.artifact().artifactId());
                assertEquals(2, guava.children().size());
                assertEquals("failureaccess", guava.children().get(0).artifact().artifactId());
            }
        }

        @Nested
        class When_ParsingWithExclusions {

            @Test
            void Then_ExcludedDependenciesAreFiltered() {
                var lines = List.of(
                        "com.example:my-project:jar:1.0.0",
                        "+- org.springframework:spring-core:jar:6.1.0:compile",
                        "+- com.mycompany:my-lib:jar:2.0.0:compile",
                        "\\- org.slf4j:slf4j-api:jar:2.0.11:compile"
                );
                var tree = resolver.parseTreeOutput(lines, ExclusionConfig.defaults());
                // Spring and SLF4J excluded, only root + my-lib remain
                assertEquals(2, tree.totalArtifactCount());
                assertEquals(1, tree.children().size());
                assertEquals("my-lib", tree.children().get(0).artifact().artifactId());
            }
        }

        @Nested
        class When_ParsingEmptyOutput {

            @Test
            void Then_ReturnsUnknownRoot() {
                var tree = resolver.parseTreeOutput(List.of(), ExclusionConfig.none());
                assertNotNull(tree);
                assertEquals("unknown", tree.artifact().groupId());
            }
        }

        @Nested
        class When_ParsingWithDuplicateCoordinates {

            @Test
            void Then_DeduplicatesByCoordinates() {
                var lines = List.of(
                        "com.example:my-project:jar:1.0.0",
                        "+- com.google.guava:guava:jar:33.0.0-jre:compile",
                        "\\- com.google.guava:guava:jar:33.0.0-jre:compile"
                );
                var tree = resolver.parseTreeOutput(lines, ExclusionConfig.none());
                // Duplicate guava should be deduped: root + 1 unique child
                assertEquals(2, tree.totalArtifactCount());
                assertEquals(1, tree.children().size());
            }
        }

        @Nested
        class When_ParsingMalformedLines {

            @Test
            void Then_SkipsBadLinesAndParsesValid() {
                var lines = List.of(
                        "com.example:my-project:jar:1.0.0",
                        "this is garbage",
                        "+- com.valid:lib-a:jar:1.0.0:compile",
                        "[INFO] some maven log line",
                        "",
                        "\\- com.valid:lib-b:jar:2.0.0:compile"
                );
                var tree = resolver.parseTreeOutput(lines, ExclusionConfig.none());
                assertEquals(3, tree.totalArtifactCount());
                assertEquals(2, tree.children().size());
            }
        }

        @Nested
        class When_ParsingWithNullExclusionConfig {

            @Test
            void Then_IncludesAllArtifacts() {
                var lines = List.of(
                        "com.example:my-project:jar:1.0.0",
                        "+- org.springframework:spring-core:jar:6.1.0:compile",
                        "\\- org.slf4j:slf4j-api:jar:2.0.11:compile"
                );
                var tree = resolver.parseTreeOutput(lines, null);
                assertEquals(3, tree.totalArtifactCount());
            }
        }
    }

    @Nested
    class Given_ResolvedArtifact {

        @Nested
        class When_GettingCoordinates {

            @Test
            void Then_FormatsCorrectly() {
                var artifact = new ResolvedArtifact("com.example", "my-lib", "1.0.0");
                assertEquals("com.example:my-lib:1.0.0", artifact.coordinates());
            }

            @Test
            void Then_IncludesClassifierWhenPresent() {
                var artifact = new ResolvedArtifact("com.example", "my-lib", "1.0.0",
                        "compile", "jar", "sources", false);
                assertEquals("com.example:my-lib:sources:1.0.0", artifact.coordinates());
            }

            @Test
            void Then_OmitsBlankClassifier() {
                var artifact = new ResolvedArtifact("com.example", "my-lib", "1.0.0",
                        "compile", "jar", "", false);
                assertEquals("com.example:my-lib:1.0.0", artifact.coordinates());
            }
        }
    }

    @Nested
    class Given_DependencyResolverRegistry {

        @Nested
        class When_DetectingBuildSystem {

            @Test
            void Then_DetectsMavenProject(@TempDir Path tempDir) throws Exception {
                Files.writeString(tempDir.resolve("pom.xml"), "<project></project>");
                var registry = new DependencyResolverRegistry(List.of(resolver));
                var detected = registry.detect(tempDir);
                assertTrue(detected.isPresent());
                assertEquals("maven", detected.get().buildSystem());
            }

            @Test
            void Then_ReturnsEmptyForUnknownProject(@TempDir Path tempDir) {
                var registry = new DependencyResolverRegistry(List.of(resolver));
                var detected = registry.detect(tempDir);
                assertTrue(detected.isEmpty());
            }
        }
    }
}
