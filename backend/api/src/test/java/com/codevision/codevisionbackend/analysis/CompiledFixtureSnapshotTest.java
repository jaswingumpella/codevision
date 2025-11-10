package com.codevision.codevisionbackend.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import com.codevision.codevisionbackend.analysis.CompiledAnalysisService.CompiledAnalysisParameters;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Utility test that can be run locally to regenerate the deterministic hashes for the compiled
 * fixture exports without booting the entire application stack.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@EnabledIfSystemProperty(named = "codevision.e2e.printHashes", matches = "true")
class CompiledFixtureSnapshotTest {

    @Autowired
    private CompiledAnalysisService compiledAnalysisService;

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:fixture-snapshot;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;NON_KEYWORDS=VALUE");
        registry.add("spring.datasource.driverClassName", () -> "org.h2.Driver");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("analysis.accept-packages", () -> "com.codevision.fixtures");
        registry.add("analysis.include-dependencies", () -> "false");
        registry.add("analysis.compile.auto", () -> "false");
    }

    @Test
    void generateExportHashes(@TempDir Path workspace) throws Exception {
        Path fixtureRoot = TestFixtures.copyCompiledFixture(workspace);
        CompiledAnalysisResult result = compiledAnalysisService.analyze(
                new CompiledAnalysisParameters(fixtureRoot, List.of("com.codevision.fixtures"), false, null));

        List<ExportedFile> exports = compiledAnalysisService.listExports(result.run().getId());
        assertThat(exports).isNotEmpty();

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        HexFormat hex = HexFormat.of();

        for (ExportedFile file : exports) {
            byte[] hash = digest.digest(Files.readAllBytes(file.path()));
            System.out.printf("%s %s%n", file.name(), hex.formatHex(hash));
        }
    }
}
