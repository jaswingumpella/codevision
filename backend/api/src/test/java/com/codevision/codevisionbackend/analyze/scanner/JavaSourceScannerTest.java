package com.codevision.codevisionbackend.analyze.scanner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codevision.codevisionbackend.analyze.scanner.ClassMetadataRecord.SourceSet;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JavaSourceScannerTest {

    private final JavaSourceScanner scanner = new JavaSourceScanner();

    @Test
    void scanCollectsAllClassesAndKeepsUserCodeFlag(@TempDir Path repoRoot) throws Exception {
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
