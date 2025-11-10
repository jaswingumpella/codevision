package com.codevision.codevisionbackend.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import com.codevision.codevisionbackend.analysis.ClasspathBuilder.ClasspathDescriptor;
import com.codevision.codevisionbackend.analysis.BytecodeCallGraphScanner.CallGraphResult;
import com.codevision.codevisionbackend.analysis.config.CompiledAnalysisProperties;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BytecodeCallGraphScannerTest {

    @TempDir
    Path tempDir;

    @Test
    void scansDirectoryEntries() throws IOException {
        Path repo = TestFixtures.copyCompiledFixture(tempDir);
        CallGraphResult result = new BytecodeCallGraphScanner(new CompiledAnalysisProperties())
                .scan(descriptor(repo, true), List.of("com.codevision.fixtures"));

        Map<String, java.util.Set<String>> adjacency = result.classAdjacency();
        assertThat(adjacency.get("com.codevision.fixtures.controller.FixtureController"))
                .contains("com.codevision.fixtures.service.FixtureService");
        assertThat(result.methodEdges())
                .anySatisfy(edge -> {
                    assertThat(edge.getCallerClass()).isEqualTo("com.codevision.fixtures.controller.FixtureController");
                    assertThat(edge.getCalleeClass()).isEqualTo("com.codevision.fixtures.service.FixtureService");
                });
    }

    @Test
    void scansJarEntries() throws IOException {
        Path repo = TestFixtures.copyCompiledFixture(tempDir);
        CallGraphResult result = new BytecodeCallGraphScanner(new CompiledAnalysisProperties())
                .scan(descriptor(repo, false), List.of("com.codevision.fixtures"));

        assertThat(result.classAdjacency()).isNotEmpty();
    }

    private ClasspathDescriptor descriptor(Path repo, boolean includeClassesDir) {
        Path classesDir = repo.resolve("target").resolve("classes");
        Path jar = repo.resolve("target").resolve("compiled-app.jar");
        List<Path> entries = includeClassesDir ? List.of(classesDir, jar) : List.of(jar);
        String cp = entries.stream().map(Path::toString).collect(Collectors.joining(System.getProperty("path.separator")));
        return new ClasspathDescriptor(repo, classesDir, entries, cp);
    }
}
