package com.codevision.codevisionbackend.analysis;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;

/**
 * Utility helpers for copying fixture repositories and bytecode jars into temporary test folders.
 */
public final class TestFixtures {

    private TestFixtures() {}

    public static Path copyCompiledFixture(Path tempDir) throws IOException {
        Path source = resolveFixtureRoot();
        Path destination = tempDir.resolve("compiled-app");
        Files.walkFileTree(source, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path target = destination.resolve(source.relativize(dir).toString());
                Files.createDirectories(target);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Path target = destination.resolve(source.relativize(file).toString());
                Files.createDirectories(target.getParent());
                Files.copy(file, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
        return destination;
    }

    private static Path resolveFixtureRoot() {
        try {
            URL resource = TestFixtures.class.getResource("/fixtures/compiled-app");
            Objects.requireNonNull(resource, "Fixture directory missing from test resources");
            return Paths.get(resource.toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Unable to resolve compiled fixture directory", e);
        }
    }
}
