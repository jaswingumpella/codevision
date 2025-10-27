package com.codevision.codevisionbackend.analyze.scanner;

import com.codevision.codevisionbackend.analyze.MetadataDump;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class YamlScanner {

    private static final Logger log = LoggerFactory.getLogger(YamlScanner.class);
    private static final Set<String> IGNORED_DIRECTORIES = Set.of(".git", "target", "build", "node_modules", ".idea", ".gradle");

    public MetadataDump scan(Path repoRoot) {
        if (!Files.exists(repoRoot)) {
            return MetadataDump.empty();
        }

        List<MetadataDump.OpenApiSpec> openApiSpecs = new ArrayList<>();
        FileVisitor<Path> visitor = new SimpleFileVisitor<>() {

            private final Set<Path> visitedDirectories = new HashSet<>();

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                Path dirName = dir.getFileName();
                if (dirName != null && IGNORED_DIRECTORIES.contains(dirName.toString())) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                Path normalized = dir.toAbsolutePath().normalize();
                if (!visitedDirectories.add(normalized)) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (!attrs.isRegularFile()) {
                    return FileVisitResult.CONTINUE;
                }
                String filename = file.getFileName().toString();
                if (isOpenApiFile(filename)) {
                    readOpenApi(file).ifPresent(openApiSpecs::add);
                }
                return FileVisitResult.CONTINUE;
            }
        };

        try {
            Files.walkFileTree(repoRoot, visitor);
        } catch (IOException e) {
            log.warn("Failed walking YAML files at {}: {}", repoRoot, e.getMessage());
        }

        if (openApiSpecs.isEmpty()) {
            return MetadataDump.empty();
        }
        return new MetadataDump(openApiSpecs);
    }

    private boolean isOpenApiFile(String filename) {
        String normalized = filename.toLowerCase(Locale.ROOT);
        return normalized.startsWith("openapi") && (normalized.endsWith(".yml") || normalized.endsWith(".yaml"));
    }

    private java.util.Optional<MetadataDump.OpenApiSpec> readOpenApi(Path file) {
        try {
            String content = Files.readString(file);
            return java.util.Optional.of(new MetadataDump.OpenApiSpec(file.getFileName().toString(), content));
        } catch (IOException e) {
            log.debug("Failed reading {}: {}", file, e.getMessage());
            return java.util.Optional.empty();
        }
    }
}
