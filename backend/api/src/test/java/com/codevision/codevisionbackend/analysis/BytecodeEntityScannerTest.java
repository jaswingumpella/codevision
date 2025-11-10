package com.codevision.codevisionbackend.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import com.codevision.codevisionbackend.analysis.ClasspathBuilder.ClasspathDescriptor;
import com.codevision.codevisionbackend.analysis.GraphModel;
import com.codevision.codevisionbackend.analysis.GraphModel.ClassNode;
import com.codevision.codevisionbackend.analysis.GraphModel.EndpointNode;
import com.codevision.codevisionbackend.analysis.GraphModel.EndpointType;
import com.codevision.codevisionbackend.analysis.config.CompiledAnalysisProperties;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BytecodeEntityScannerTest {

    private final CompiledAnalysisProperties properties = new CompiledAnalysisProperties();

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        properties.setAcceptPackages(List.of("com.codevision.fixtures"));
    }

    @Test
    void scansEntitiesSequencesAndEndpoints() throws IOException {
        Path repo = TestFixtures.copyCompiledFixture(tempDir);
        ClasspathDescriptor descriptor = descriptor(repo);
        BytecodeEntityScanner scanner = new BytecodeEntityScanner(properties);

        GraphModel model = scanner.scan(descriptor, properties.getAcceptPackages());

        assertThat(model.getClasses()).containsKey("com.codevision.fixtures.domain.FixtureEntity");
        ClassNode entity = model.getClasses().get("com.codevision.fixtures.domain.FixtureEntity");
        assertThat(entity.isEntity()).isTrue();
        assertThat(entity.getTableName()).isEqualTo("fixture_entities");
        ClassNode controller = model.getClasses().get("com.codevision.fixtures.controller.FixtureController");
        assertThat(controller.getStereotypes()).contains("CONTROLLER");

        assertThat(model.getSequences()).containsKey("fixture_seq");
        GraphModel.SequenceNode sequence = model.getSequences().get("fixture_seq");
        assertThat(sequence.getAllocationSize()).isEqualTo(50);
        assertThat(sequence.getInitialValue()).isEqualTo(200);
        assertThat(model.getEndpoints())
                .anyMatch(endpoint -> "GET".equals(endpoint.getHttpMethod())
                        && endpoint.getPath() != null
                        && endpoint.getPath().startsWith("/fixtures"));

        assertThat(model.getEndpoints())
                .anyMatch(endpoint -> endpoint.getType() == EndpointType.KAFKA
                        && "topics=fixtures.events,audit.events".equals(endpoint.getPath()));
        assertThat(model.getEndpoints())
                .anyMatch(endpoint -> endpoint.getType() == EndpointType.SCHEDULED
                        && endpoint.getPath().startsWith("cron="));

        ClassNode config = model.getClasses().get("com.codevision.fixtures.config.FixtureConfig");
        assertThat(config.getStereotypes()).contains("CONFIGURATION", "BEAN_FACTORY");
    }

    private ClasspathDescriptor descriptor(Path repo) {
        Path classesDir = repo.resolve("target").resolve("classes");
        Path jar = repo.resolve("target").resolve("compiled-app.jar");
        List<Path> entries = List.of(classesDir, jar);
        String cp = entries.stream().map(Path::toString).collect(Collectors.joining(System.getProperty("path.separator")));
        return new ClasspathDescriptor(repo, classesDir, entries, cp);
    }
}
