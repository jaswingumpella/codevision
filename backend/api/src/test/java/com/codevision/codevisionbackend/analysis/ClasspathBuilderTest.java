package com.codevision.codevisionbackend.analysis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codevision.codevisionbackend.analysis.ClasspathBuilder.ClasspathDescriptor;
import com.codevision.codevisionbackend.analysis.config.CompiledAnalysisProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ClasspathBuilderTest {

    @TempDir
    Path tempDir;

    @Test
    void buildIncludesClassesAndFiltersDependencies() throws IOException {
        Path fixtureRoot = TestFixtures.copyCompiledFixture(tempDir);
        Path classesDir = fixtureRoot.resolve("target").resolve("classes");
        Path businessJar = fixtureRoot.resolve("target").resolve("lib").resolve("business-lib.jar").toAbsolutePath();
        Path mockitoJar = fixtureRoot.resolve("target").resolve("lib").resolve("mockito-helper.jar").toAbsolutePath();
        Path classpathFile = fixtureRoot.resolve("target").resolve("classpath.txt");
        String separator = System.getProperty("path.separator");
        Files.writeString(
                classpathFile,
                mockitoJar + separator + businessJar + separator + fixtureRoot.resolve("target").resolve("compiled-app.jar"));

        RecordingRunner runner = new RecordingRunner();
        CompiledAnalysisProperties properties = new CompiledAnalysisProperties();
        ClasspathBuilder builder = new ClasspathBuilder(properties, runner);

        ClasspathDescriptor descriptor = builder.build(fixtureRoot, true);

        assertThat(descriptor.getClassesDirectory()).isEqualTo(classesDir);
        assertThat(descriptor.getClasspathEntries())
                .contains(classesDir)
                .contains(businessJar)
                .doesNotContain(mockitoJar);
        assertThat(descriptor.getClasspathString()).contains(classesDir.toString()).contains(businessJar.toString());
        assertThat(runner.commands).hasSize(1);
        assertThat(runner.commands.get(0)).contains("dependency:build-classpath");
    }

    @Test
    void buildPropagatesRunnerFailure() throws IOException {
        Path fixtureRoot = TestFixtures.copyCompiledFixture(tempDir);
        Files.createDirectories(fixtureRoot.resolve("target").resolve("classes"));
        Files.writeString(fixtureRoot.resolve("target").resolve("classpath.txt"), "");

        FailingRunner runner = new FailingRunner();
        CompiledAnalysisProperties properties = new CompiledAnalysisProperties();
        ClasspathBuilder builder = new ClasspathBuilder(properties, runner);

        assertThatThrownBy(() -> builder.build(fixtureRoot, true))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("timeout");
    }

    private static final class RecordingRunner extends MavenCommandRunner {
        private final List<List<String>> commands = new ArrayList<>();

        @Override
        public void run(Path workingDir, List<String> command, java.time.Duration timeout, int maxHeapMb) {
            commands.add(List.copyOf(command));
        }
    }

    private static final class FailingRunner extends MavenCommandRunner {
        @Override
        public void run(Path workingDir, List<String> command, java.time.Duration timeout, int maxHeapMb) {
            throw new IllegalStateException("timeout");
        }
    }
}
