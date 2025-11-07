package com.codevision.codevisionbackend.analyze.scanner;

import com.codevision.codevisionbackend.config.SecurityScanProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

class PiiPciInspectorTest {

    @Test
    void assessTextDetectsRiskWhenRulesMatch() {
        SecurityScanProperties properties = new SecurityScanProperties();
        SecurityScanProperties.Rule piiRule = new SecurityScanProperties.Rule();
        piiRule.setKeyword("password");
        piiRule.setType("PII");
        piiRule.setSeverity("MEDIUM");
        SecurityScanProperties.Rule pciRule = new SecurityScanProperties.Rule();
        pciRule.setRegex("\\b\\d{16}\\b");
        pciRule.setType("PCI");
        pciRule.setSeverity("HIGH");
        properties.setRules(List.of(piiRule, pciRule));

        PiiPciInspector inspector = new PiiPciInspector(properties);

        PiiPciInspector.RiskAssessment assessment =
                inspector.assessText("Persisting password for card 4111111111111111");

        assertThat(assessment.piiRisk()).isTrue();
        assertThat(assessment.pciRisk()).isTrue();
    }

    @Test
    void scanMarksIgnoredFindings(@TempDir Path tempDir) throws IOException {
        SecurityScanProperties properties = new SecurityScanProperties();
        SecurityScanProperties.Rule rule = new SecurityScanProperties.Rule();
        rule.setKeyword("secret");
        rule.setType("PII");
        rule.setSeverity("LOW");
        properties.setRules(List.of(rule));
        properties.setIgnorePatterns(List.of(".*sample.txt"));

        Path file = tempDir.resolve("sample.txt");
        Files.writeString(file, "apiSecret = secret-value");

        PiiPciInspector inspector = new PiiPciInspector(properties);

        List<PiiPciFindingRecord> findings = inspector.scan(tempDir);

        assertThat(findings).hasSize(1);
        PiiPciFindingRecord record = findings.get(0);
        assertThat(record.filePath()).endsWith("sample.txt");
        assertThat(record.ignored()).isTrue();
        assertThat(record.matchType()).isEqualTo("PII");
    }
}
