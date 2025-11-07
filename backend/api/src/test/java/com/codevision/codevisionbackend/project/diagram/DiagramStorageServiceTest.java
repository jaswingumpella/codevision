package com.codevision.codevisionbackend.project.diagram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.codevision.codevisionbackend.config.DiagramStorageProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DiagramStorageServiceTest {

    @TempDir
    Path tempDir;

    private DiagramStorageService storageService;

    @BeforeEach
    void setUp() {
        DiagramStorageProperties properties = new DiagramStorageProperties();
        properties.setRoot(tempDir.toString());
        storageService = new DiagramStorageService(properties);
        storageService.prepareRoot();
    }

    @Test
    void storeLoadAndDeleteSvgRoundTripsContent() throws IOException {
        byte[] svg = "<svg>demo</svg>".getBytes();

        String relativePath = storageService.storeSvg(42L, DiagramType.CLASS, 0, svg);
        assertThat(relativePath).contains("project-42").contains("class/diagram-0.svg");

        Path storedFile = tempDir.resolve(relativePath);
        assertThat(Files.exists(storedFile)).isTrue();
        assertThat(Files.readAllBytes(storedFile)).containsExactly(svg);

        byte[] loaded = storageService.loadSvg(relativePath);
        assertThat(loaded).containsExactly(svg);

        storageService.deleteSvg(relativePath);
        assertThat(Files.exists(storedFile)).isFalse();
    }

    @Test
    void purgeProjectRemovesNestedDirectories() {
        storageService.storeSvg(7L, DiagramType.SEQUENCE, 3, "<svg/>".getBytes());

        storageService.purgeProject(7L);

        Path projectDir = tempDir.resolve("project-7");
        assertThat(Files.exists(projectDir)).isFalse();
    }

    @Test
    void preventsAccessOutsideStorageRoot() {
        assertThrows(IllegalArgumentException.class, () -> storageService.loadSvg("../outside.svg"));
        storageService.deleteSvg("../outside.svg");
    }
}
