package com.codevision.codevisionbackend.analysis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.codevision.codevisionbackend.analysis.GraphModel.ClassNode;
import com.codevision.codevisionbackend.analysis.GraphModel.DependencyEdge;
import com.codevision.codevisionbackend.analysis.GraphModel.DependencyKind;
import com.codevision.codevisionbackend.analysis.GraphModel.EndpointNode;
import com.codevision.codevisionbackend.analysis.GraphModel.EndpointType;
import com.codevision.codevisionbackend.analysis.GraphModel.FieldModel;
import com.codevision.codevisionbackend.analysis.GraphModel.Origin;
import com.codevision.codevisionbackend.analysis.GraphModel.SequenceNode;
import com.codevision.codevisionbackend.analysis.GraphModel.SequenceUsage;
import com.codevision.codevisionbackend.analysis.config.CompiledAnalysisProperties;
import com.codevision.codevisionbackend.analysis.persistence.AnalysisEntityFieldRepository;
import com.codevision.codevisionbackend.analysis.persistence.AnalysisEntityRecord;
import com.codevision.codevisionbackend.analysis.persistence.AnalysisEntityRepository;
import com.codevision.codevisionbackend.analysis.persistence.ClassDependencyRecord;
import com.codevision.codevisionbackend.analysis.persistence.ClassDependencyRepository;
import com.codevision.codevisionbackend.analysis.persistence.CompiledEndpointRecord;
import com.codevision.codevisionbackend.analysis.persistence.CompiledEndpointRepository;
import com.codevision.codevisionbackend.analysis.persistence.EntitySequenceUsageRepository;
import com.codevision.codevisionbackend.analysis.persistence.SequenceRecordRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PersistServiceTest {

    @Mock
    private AnalysisEntityRepository entityRepository;

    @Mock
    private AnalysisEntityFieldRepository fieldRepository;

    @Mock
    private SequenceRecordRepository sequenceRecordRepository;

    @Mock
    private EntitySequenceUsageRepository entitySequenceUsageRepository;

    @Mock
    private ClassDependencyRepository classDependencyRepository;

    @Mock
    private CompiledEndpointRepository compiledEndpointRepository;

    private PersistService persistService;

    @BeforeEach
    void setUp() {
        CompiledAnalysisProperties properties = new CompiledAnalysisProperties();
        properties.setAcceptPackages(List.of("com.example"));
        persistService = new PersistService(
                entityRepository,
                fieldRepository,
                sequenceRecordRepository,
                entitySequenceUsageRepository,
                classDependencyRepository,
                compiledEndpointRepository,
                properties);
    }

    @Test
    void persistsGraphModelToRepositories() {
        GraphModel model = GraphModel.empty();
        ClassNode classNode = new ClassNode();
        classNode.setName("com.example.Entity");
        classNode.setPackageName("com.example");
        classNode.setOrigin(Origin.BYTECODE);
        FieldModel field = new FieldModel();
        field.setName("id");
        field.setType("java.lang.Long");
        classNode.getFields().add(field);
        model.addClass(classNode);

        SequenceNode sequenceNode = new SequenceNode();
        sequenceNode.setGeneratorName("entity_seq");
        sequenceNode.setSequenceName("entity_sequence");
        model.addSequence(sequenceNode);
        model.addSequenceUsage(new SequenceUsage("com.example.Entity", "id", "entity_seq"));

        model.addDependency(new DependencyEdge(
                DependencyKind.CALL, "com.example.Controller", "com.example.Entity", "call"));
        model.addDependency(new DependencyEdge(
                DependencyKind.EXTENDS, "com.example.Controller", "java.lang.Object", "extends"));

        EndpointNode endpointNode = new EndpointNode();
        endpointNode.setType(EndpointType.HTTP);
        endpointNode.setPath("/entities");
        endpointNode.setControllerClass("com.example.Controller");
        endpointNode.setControllerMethod("get");
        endpointNode.setHttpMethod("GET");
        model.addEndpoint(endpointNode);

        persistService.persist(model);

        verify(entitySequenceUsageRepository).deleteAllInBatch();
        verify(fieldRepository).deleteAllInBatch();
        verify(classDependencyRepository).deleteAllInBatch();
        verify(compiledEndpointRepository).deleteAllInBatch();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<AnalysisEntityRecord>> entitiesCaptor = ArgumentCaptor.forClass(List.class);
        verify(entityRepository).saveAll(entitiesCaptor.capture());
        assertThat(entitiesCaptor.getValue()).hasSize(1);
        assertThat(entitiesCaptor.getValue().get(0).getClassName()).isEqualTo("com.example.Entity");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ClassDependencyRecord>> dependencyCaptor = ArgumentCaptor.forClass(List.class);
        verify(classDependencyRepository).saveAll(dependencyCaptor.capture());
        assertThat(dependencyCaptor.getValue()).hasSize(1);
        assertThat(dependencyCaptor.getValue().get(0).getId().getCaller()).isEqualTo("com.example.Controller");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CompiledEndpointRecord>> endpointCaptor = ArgumentCaptor.forClass(List.class);
        verify(compiledEndpointRepository).saveAll(endpointCaptor.capture());
        assertThat(endpointCaptor.getValue()).hasSize(1);
        assertThat(endpointCaptor.getValue().get(0).getPath()).isEqualTo("/entities");
    }
}
