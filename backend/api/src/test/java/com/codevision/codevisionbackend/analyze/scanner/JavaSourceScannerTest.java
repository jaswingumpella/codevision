package com.codevision.codevisionbackend.analyze.scanner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
        Path mainSource = repoRoot.resolve("src/main/java/com/barclays/demo");
        Path externalSource = repoRoot.resolve("src/main/java/com/example/external");
        Path testSource = repoRoot.resolve("src/test/java/com/barclays/demo");
        Files.createDirectories(mainSource);
        Files.createDirectories(externalSource);
        Files.createDirectories(testSource);

        Files.writeString(
                mainSource.resolve("MyController.java"),
                """
                package com.barclays.demo;

                import org.springframework.web.bind.annotation.RestController;

                @RestController
                public class MyController {
                }
                """);

        Files.writeString(
                mainSource.resolve("MyService.java"),
                """
                package com.barclays.demo;

                import org.springframework.stereotype.Service;

                @Service
                public class MyService {
                }
                """);

        Files.writeString(
                testSource.resolve("MyServiceTest.java"),
                """
                package com.barclays.demo;

                import org.junit.jupiter.api.Test;

                public class MyServiceTest {
                    @Test
                    void sample() {
                    }
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
        assertEquals(4, records.size());

        Optional<ClassMetadataRecord> controller =
                records.stream().filter(record -> record.className().equals("MyController")).findFirst();
        Optional<ClassMetadataRecord> service =
                records.stream().filter(record -> record.className().equals("MyService")).findFirst();
        Optional<ClassMetadataRecord> test =
                records.stream().filter(record -> record.className().equals("MyServiceTest")).findFirst();
        Optional<ClassMetadataRecord> external =
                records.stream().filter(record -> record.className().equals("ExternalClass")).findFirst();

        assertTrue(controller.isPresent());
        assertEquals("CONTROLLER", controller.get().stereotype());
        assertEquals(SourceSet.MAIN, controller.get().sourceSet());
        assertEquals("src/main/java/com/barclays/demo/MyController.java", controller.get().relativePath());
        assertTrue(controller.get().userCode());

        assertTrue(service.isPresent());
        assertEquals("SERVICE", service.get().stereotype());
        assertTrue(service.get().userCode());

        assertTrue(test.isPresent());
        assertEquals("TEST", test.get().stereotype());
        assertEquals(SourceSet.TEST, test.get().sourceSet());
        assertTrue(test.get().userCode());

        assertTrue(external.isPresent());
        assertEquals("OTHER", external.get().stereotype());
        assertFalse(external.get().userCode());
    }
}
