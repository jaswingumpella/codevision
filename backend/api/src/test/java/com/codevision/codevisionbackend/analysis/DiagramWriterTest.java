package com.codevision.codevisionbackend.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import com.codevision.codevisionbackend.analysis.GraphModel.ClassNode;
import com.codevision.codevisionbackend.analysis.GraphModel.DependencyEdge;
import com.codevision.codevisionbackend.analysis.GraphModel.DependencyKind;
import com.codevision.codevisionbackend.analysis.GraphModel.EndpointNode;
import com.codevision.codevisionbackend.analysis.GraphModel.EndpointType;
import com.codevision.codevisionbackend.analysis.GraphModel.FieldModel;
import com.codevision.codevisionbackend.analysis.GraphModel.MethodCallEdge;
import com.codevision.codevisionbackend.analysis.config.CompiledAnalysisProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DiagramWriterTest {

    private final DiagramWriter writer = new DiagramWriter();

    @TempDir
    Path tempDir;

    @Test
    void writesClassErdAndSequenceDiagrams() throws IOException {
        GraphModel model = buildModel();
        CompiledAnalysisProperties properties = new CompiledAnalysisProperties();
        properties.setMaxCallDepth(4);

        DiagramWriter.DiagramArtifacts artifacts = writer.writeDiagrams(model, tempDir, properties);

        assertThat(artifacts.classDiagram()).exists();
        assertThat(Files.readString(artifacts.classDiagram()))
                .contains("<<CONTROLLER, CYCLE>>")
                .contains("*serviceField");
        assertThat(artifacts.erdPlantUml()).exists();
        assertThat(Files.readString(artifacts.erdPlantUml())).contains("entity com.example.Service");
        assertThat(artifacts.erdMermaid()).exists();
        assertThat(Files.readString(artifacts.erdMermaid())).contains("Service ||--o{ com.example.Repository");
        assertThat(artifacts.sequenceDiagrams()).hasSize(1);
        Path sequenceFile = artifacts.sequenceDiagrams().get(0);
        assertThat(Files.readString(sequenceFile)).contains("Client -> com.example.Controller");
    }

    private GraphModel buildModel() {
        GraphModel model = GraphModel.empty();
        ClassNode controller = new ClassNode();
        controller.setName("com.example.Controller");
        controller.getStereotypes().add("CONTROLLER");
        controller.setInCycle(true);
        controller.setSccId(1L);
        FieldModel injectedField = new FieldModel();
        injectedField.setName("serviceField");
        injectedField.setType("Lcom/example/Service;");
        injectedField.setInjected(true);
        controller.getFields().add(injectedField);

        ClassNode service = new ClassNode();
        service.setName("com.example.Service");
        service.setEntity(true);
        FieldModel relationField = new FieldModel();
        relationField.setName("repo");
        relationField.setType("Lcom/example/Repository;");
        relationField.setRelationship(true);
        service.getFields().add(relationField);

        model.addClass(controller);
        model.addClass(service);
        model.addDependency(new DependencyEdge(DependencyKind.CALL, controller.getName(), service.getName(), "call"));
        model.addMethodCallEdge(new MethodCallEdge(
                controller.getName(), "get", "()V", service.getName(), "save", "()V"));

        EndpointNode endpoint = new EndpointNode();
        endpoint.setType(EndpointType.HTTP);
        endpoint.setControllerClass(controller.getName());
        endpoint.setControllerMethod("get");
        endpoint.setHttpMethod("GET");
        endpoint.setPath("/demo");
        model.addEndpoint(endpoint);
        return model;
    }
}
