package com.codevision.codevisionbackend.project.diagram;

import com.codevision.codevisionbackend.config.DiagramStorageProperties;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DiagramStorageService {

    private static final Logger log = LoggerFactory.getLogger(DiagramStorageService.class);

    private final Path storageRoot;

    public DiagramStorageService(DiagramStorageProperties properties) {
        this.storageRoot = properties.resolveRoot();
    }

    @PostConstruct
    void prepareRoot() {
        try {
            Files.createDirectories(storageRoot);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to initialise diagram storage root " + storageRoot, e);
        }
    }

    public String storeSvg(Long projectId, DiagramType diagramType, int sequence, byte[] svgContent) {
        if (svgContent == null || svgContent.length == 0) {
            return null;
        }
        Path targetDir = storageRoot.resolve("project-" + projectId).resolve(diagramType.name().toLowerCase());
        try {
            Files.createDirectories(targetDir);
            String fileName = "diagram-" + sequence + ".svg";
            Path targetFile = targetDir.resolve(fileName);
            Files.write(targetFile, svgContent, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            return storageRoot.relativize(targetFile).toString().replace('\\', '/');
        } catch (IOException e) {
            log.warn("Failed to store diagram svg for project {} type {}", projectId, diagramType, e);
            return null;
        }
    }

    public byte[] loadSvg(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return null;
        }
        Path target = storageRoot.resolve(relativePath).normalize();
        if (!target.startsWith(storageRoot)) {
            throw new IllegalArgumentException("Attempted to read SVG outside storage root");
        }
        try {
            if (!Files.exists(target)) {
                return null;
            }
            return Files.readAllBytes(target);
        } catch (IOException e) {
            log.warn("Failed to read diagram svg {}", relativePath, e);
            return null;
        }
    }

    public void deleteSvg(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return;
        }
        Path target = storageRoot.resolve(relativePath).normalize();
        if (!target.startsWith(storageRoot)) {
            return;
        }
        try {
            Files.deleteIfExists(target);
        } catch (IOException e) {
            log.warn("Failed to delete diagram svg {}", relativePath, e);
        }
    }

    public void purgeProject(Long projectId) {
        Path projectDir = storageRoot.resolve("project-" + projectId);
        deleteRecursively(projectDir);
    }

    private void deleteRecursively(Path path) {
        if (path == null || !Files.exists(path)) {
            return;
        }
        try (var stream = Files.list(path)) {
            stream.forEach(child -> {
                if (Files.isDirectory(child)) {
                    deleteRecursively(child);
                } else {
                    try {
                        Files.deleteIfExists(child);
                    } catch (IOException e) {
                        log.debug("Failed to delete {}", child, e);
                    }
                }
            });
        } catch (IOException e) {
            log.debug("Failed to traverse {}", path, e);
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.debug("Failed to delete directory {}", path, e);
        }
    }
}
