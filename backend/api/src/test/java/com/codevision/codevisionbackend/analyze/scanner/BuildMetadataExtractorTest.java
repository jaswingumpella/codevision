package com.codevision.codevisionbackend.analyze.scanner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codevision.codevisionbackend.analyze.BuildInfo;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BuildMetadataExtractorTest {

    private final BuildMetadataExtractor extractor = new BuildMetadataExtractor();

    @Test
    void extractReturnsBuildInfoAndModules(@TempDir Path tempDir) throws Exception {
        Path moduleDir = tempDir.resolve("module-a");
        Files.createDirectories(moduleDir);

        Path rootPom = tempDir.resolve("pom.xml");
        Files.writeString(
                rootPom,
                """
                <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.barclays</groupId>
                  <artifactId>demo-app</artifactId>
                  <version>1.2.3</version>
                  <properties>
                    <java.version>17</java.version>
                  </properties>
                  <modules>
                    <module>module-a</module>
                  </modules>
                </project>
                """);

        Path modulePom = moduleDir.resolve("pom.xml");
        Files.writeString(
                modulePom,
                """
                <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                  <modelVersion>4.0.0</modelVersion>
                  <parent>
                    <groupId>com.barclays</groupId>
                    <artifactId>demo-app</artifactId>
                    <version>1.2.3</version>
                  </parent>
                  <artifactId>module-a</artifactId>
                </project>
                """);

        BuildMetadataExtractor.BuildMetadata metadata = extractor.extract(tempDir);

        BuildInfo buildInfo = metadata.buildInfo();
        assertEquals("com.barclays", buildInfo.groupId());
        assertEquals("demo-app", buildInfo.artifactId());
        assertEquals("1.2.3", buildInfo.version());
        assertEquals("17", buildInfo.javaVersion());

        List<Path> moduleRoots = metadata.moduleRoots();
        assertEquals(2, moduleRoots.size());
        assertTrue(moduleRoots.contains(tempDir.toAbsolutePath()));
        assertTrue(moduleRoots.contains(moduleDir.toAbsolutePath()));
    }

    @Test
    void extractInfersBuildInfoFromNestedModuleWhenRootPomMissing(@TempDir Path tempDir) throws Exception {
        Path nested = tempDir.resolve("service");
        Files.createDirectories(nested);

        Path modulePom = nested.resolve("pom.xml");
        Files.writeString(
                modulePom,
                """
                <project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"
                         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.example</groupId>
                  <artifactId>nested-service</artifactId>
                  <version>0.9.0</version>
                  <properties>
                    <java.version>21</java.version>
                  </properties>
                </project>
                """);

        BuildMetadataExtractor.BuildMetadata metadata = extractor.extract(tempDir);

        BuildInfo buildInfo = metadata.buildInfo();
        assertEquals("com.example", buildInfo.groupId());
        assertEquals("nested-service", buildInfo.artifactId());
        assertEquals("0.9.0", buildInfo.version());
        assertEquals("21", buildInfo.javaVersion());
        assertTrue(metadata.moduleRoots().contains(nested.toAbsolutePath()));
    }
}
