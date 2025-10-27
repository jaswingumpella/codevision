package com.codevision.codevisionbackend.analyze.scanner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codevision.codevisionbackend.analyze.MetadataDump;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class YamlScannerTest {

    private final YamlScanner yamlScanner = new YamlScanner();

    @Test
    void scanCapturesOpenApiSpecifications(@TempDir Path repoRoot) throws Exception {
        Files.writeString(
                repoRoot.resolve("openapi-spec.yml"),
                """
                openapi: 3.0.0
                info:
                  title: Demo
                  version: 1.0.0
                """);

        Files.writeString(
                repoRoot.resolve("application.yml"),
                "spring:\\n  application:\\n    name: demo");

        MetadataDump dump = yamlScanner.scan(repoRoot);

        assertEquals(1, dump.openApiSpecs().size());
        MetadataDump.OpenApiSpec spec = dump.openApiSpecs().get(0);
        assertEquals("openapi-spec.yml", spec.fileName());
        assertTrue(spec.content().contains("openapi: 3.0.0"));
    }
}
