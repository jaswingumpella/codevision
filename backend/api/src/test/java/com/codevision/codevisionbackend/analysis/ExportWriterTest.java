package com.codevision.codevisionbackend.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import com.codevision.codevisionbackend.analysis.GraphModel.ClassNode;
import com.codevision.codevisionbackend.analysis.GraphModel.DependencyEdge;
import com.codevision.codevisionbackend.analysis.GraphModel.DependencyKind;
import com.codevision.codevisionbackend.analysis.GraphModel.EndpointNode;
import com.codevision.codevisionbackend.analysis.GraphModel.EndpointType;
import com.codevision.codevisionbackend.analysis.GraphModel.SequenceNode;
import com.codevision.codevisionbackend.analysis.AnalysisOutputPaths;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ExportWriterTest {

    private final ExportWriter writer = new ExportWriter(new ObjectMapper());

    @TempDir
    Path tempDir;

    @Test
    void writesJsonAndCsvExports() throws IOException {
        GraphModel model = GraphModel.empty();
        ClassNode node = new ClassNode();
        node.setName("com.example.Controller");
        node.setPackageName("com.example");
        node.setTableName("demo_table");
        node.setOrigin(GraphModel.Origin.BYTECODE);
        node.getStereotypes().add("CONTROLLER");
        model.addClass(node);

        SequenceNode sequence = new SequenceNode();
        sequence.setGeneratorName("seq");
        sequence.setSequenceName("seq_table");
        model.addSequence(sequence);
        model.addEndpoint(endpoint());
        model.addDependency(new DependencyEdge(DependencyKind.CALL, "com.example.Controller", "com.example.Service", "call"));

        DiagramWriter.DiagramArtifacts artifacts = new DiagramWriter.DiagramArtifacts(
                createFile("class-diagram.puml"),
                createFile("erd.puml"),
                createFile("erd.mmd"),
                new ArrayList<>());

        AnalysisOutputPaths paths = writer.writeAll(model, tempDir.resolve("out"), artifacts);

        assertThat(paths.getAnalysisJson()).exists();
        String json = Files.readString(paths.getAnalysisJson());
        assertThat(json).contains("com.example.Controller");

        assertThat(Files.readString(paths.getEntitiesCsv(), StandardCharsets.UTF_8))
                .contains("className")
                .contains("com.example.Controller");
        assertThat(Files.readString(paths.getSequencesCsv(), StandardCharsets.UTF_8)).contains("seq_table");
        assertThat(Files.readString(paths.getEndpointsCsv(), StandardCharsets.UTF_8)).contains("/api/demo");
    }

    private Path createFile(String name) throws IOException {
        Path file = tempDir.resolve(name);
        Files.createDirectories(file.getParent() != null ? file.getParent() : tempDir);
        Files.writeString(file, name);
        return file;
    }

    private EndpointNode endpoint() {
        EndpointNode endpoint = new EndpointNode();
        endpoint.setType(EndpointType.HTTP);
        endpoint.setPath("/api/demo");
        endpoint.setControllerClass("com.example.Controller");
        endpoint.setControllerMethod("get");
        endpoint.setHttpMethod("GET");
        endpoint.setFramework("SPRING_MVC");
        return endpoint;
    }
}
