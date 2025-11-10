package com.codevision.codevisionbackend.analysis;

import com.codevision.codevisionbackend.analysis.GraphModel.DependencyEdge;
import com.codevision.codevisionbackend.analysis.GraphModel.DependencyKind;
import com.codevision.codevisionbackend.analysis.GraphModel.EndpointNode;
import com.codevision.codevisionbackend.analysis.GraphModel.SequenceNode;
import com.codevision.codevisionbackend.analysis.GraphModel.SequenceUsage;
import com.codevision.codevisionbackend.analysis.config.CompiledAnalysisProperties;
import com.codevision.codevisionbackend.analysis.persistence.AnalysisEntityFieldId;
import com.codevision.codevisionbackend.analysis.persistence.AnalysisEntityFieldRecord;
import com.codevision.codevisionbackend.analysis.persistence.AnalysisEntityFieldRepository;
import com.codevision.codevisionbackend.analysis.persistence.AnalysisEntityRecord;
import com.codevision.codevisionbackend.analysis.persistence.AnalysisEntityRepository;
import com.codevision.codevisionbackend.analysis.persistence.ClassDependencyId;
import com.codevision.codevisionbackend.analysis.persistence.ClassDependencyRecord;
import com.codevision.codevisionbackend.analysis.persistence.ClassDependencyRepository;
import com.codevision.codevisionbackend.analysis.persistence.CompiledEndpointRecord;
import com.codevision.codevisionbackend.analysis.persistence.CompiledEndpointRepository;
import com.codevision.codevisionbackend.analysis.persistence.EntitySequenceUsageId;
import com.codevision.codevisionbackend.analysis.persistence.EntitySequenceUsageRecord;
import com.codevision.codevisionbackend.analysis.persistence.EntitySequenceUsageRepository;
import com.codevision.codevisionbackend.analysis.persistence.SequenceRecord;
import com.codevision.codevisionbackend.analysis.persistence.SequenceRecordRepository;
import jakarta.transaction.Transactional;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Persists the compiled analysis summary into Postgres tables for UI queries.
 */
@Service
public class PersistService {

    private final AnalysisEntityRepository entityRepository;
    private final AnalysisEntityFieldRepository fieldRepository;
    private final SequenceRecordRepository sequenceRepository;
    private final EntitySequenceUsageRepository entitySequenceUsageRepository;
    private final ClassDependencyRepository classDependencyRepository;
    private final CompiledEndpointRepository endpointRepository;
    private final CompiledAnalysisProperties properties;

    public PersistService(
            AnalysisEntityRepository entityRepository,
            AnalysisEntityFieldRepository fieldRepository,
            SequenceRecordRepository sequenceRepository,
            EntitySequenceUsageRepository entitySequenceUsageRepository,
            ClassDependencyRepository classDependencyRepository,
            CompiledEndpointRepository endpointRepository,
            CompiledAnalysisProperties properties) {
        this.entityRepository = entityRepository;
        this.fieldRepository = fieldRepository;
        this.sequenceRepository = sequenceRepository;
        this.entitySequenceUsageRepository = entitySequenceUsageRepository;
        this.classDependencyRepository = classDependencyRepository;
        this.endpointRepository = endpointRepository;
        this.properties = properties;
    }

    @Transactional
    public void persist(GraphModel model) {
        entitySequenceUsageRepository.deleteAllInBatch();
        fieldRepository.deleteAllInBatch();
        classDependencyRepository.deleteAllInBatch();
        endpointRepository.deleteAllInBatch();
        sequenceRepository.deleteAllInBatch();
        entityRepository.deleteAllInBatch();

        Map<String, AnalysisEntityRecord> entities = persistEntities(model);
        persistFields(model, entities);
        Map<String, SequenceRecord> sequences = persistSequences(model);
        persistSequenceUsage(model, entities, sequences);
        persistDependencies(model);
        persistEndpoints(model);
    }

    private Map<String, AnalysisEntityRecord> persistEntities(GraphModel model) {
        List<AnalysisEntityRecord> entities = model.sortedClasses().stream()
                .map(node -> {
                    AnalysisEntityRecord record = new AnalysisEntityRecord();
                    record.setClassName(node.getName());
                    record.setPackageName(node.getPackageName());
                    record.setJarOrDir(node.getJarOrDirectory());
                    record.setTableName(node.getTableName());
                    record.setOrigin(node.getOrigin());
                    record.setSccId(node.getSccId());
                    record.setInCycle(node.isInCycle());
                    return record;
                })
                .toList();
        entityRepository.saveAll(entities);
        return entities.stream().collect(Collectors.toMap(AnalysisEntityRecord::getClassName, record -> record));
    }

    private void persistFields(GraphModel model, Map<String, AnalysisEntityRecord> entities) {
        List<AnalysisEntityFieldRecord> fields = model.sortedClasses().stream()
                .flatMap(node -> node.getFields().stream()
                        .map(field -> {
                            AnalysisEntityRecord entity = entities.get(node.getName());
                            if (entity == null) {
                                return null;
                            }
                            AnalysisEntityFieldRecord record = new AnalysisEntityFieldRecord();
                            record.setId(new AnalysisEntityFieldId(entity.getId(), field.getName()));
                            record.setEntity(entity);
                            record.setType(field.getType());
                            record.setJoinField(field.isRelationship());
                            return record;
                        }))
                .filter(Objects::nonNull)
                .toList();
        fieldRepository.saveAll(fields);
    }

    private Map<String, SequenceRecord> persistSequences(GraphModel model) {
        List<SequenceRecord> sequences = model.getSequences().values().stream()
                .map(node -> {
                    SequenceRecord record = new SequenceRecord();
                    record.setGeneratorName(node.getGeneratorName());
                    record.setSequenceName(node.getSequenceName());
                    record.setAllocationSize(node.getAllocationSize());
                    record.setInitialValue(node.getInitialValue());
                    return record;
                })
                .toList();
        sequenceRepository.saveAll(sequences);
        return sequences.stream().collect(Collectors.toMap(SequenceRecord::getGeneratorName, record -> record));
    }

    private void persistSequenceUsage(
            GraphModel model, Map<String, AnalysisEntityRecord> entities, Map<String, SequenceRecord> sequences) {
        List<EntitySequenceUsageRecord> usages = model.getSequenceUsages().stream()
                .map(usage -> {
                    AnalysisEntityRecord entity = entities.get(usage.getClassName());
                    SequenceRecord sequence = sequences.get(usage.getGeneratorName());
                    if (entity == null || sequence == null) {
                        return null;
                    }
                    EntitySequenceUsageRecord record = new EntitySequenceUsageRecord();
                    record.setId(new EntitySequenceUsageId(entity.getId(), usage.getFieldName()));
                    record.setEntity(entity);
                    record.setSequence(sequence);
                    return record;
                })
                .filter(Objects::nonNull)
                .toList();
        entitySequenceUsageRepository.saveAll(usages);
    }

    private void persistDependencies(GraphModel model) {
        String packageFilter = String.join(",", properties.getAcceptPackages());
        List<ClassDependencyRecord> dependencies = model.getDependencyEdges().stream()
                .filter(edge -> edge.getKind() == DependencyKind.CALL)
                .map(edge -> new ClassDependencyRecord(
                        new ClassDependencyId(edge.getFromClass(), edge.getToClass()), packageFilter))
                .toList();
        classDependencyRepository.saveAll(dependencies);
    }

    private void persistEndpoints(GraphModel model) {
        List<CompiledEndpointRecord> records = model.getEndpoints().stream()
                .map(endpoint -> {
                    CompiledEndpointRecord record = new CompiledEndpointRecord();
                    record.setType(endpoint.getType());
                    record.setHttpMethod(endpoint.getHttpMethod());
                    record.setPath(endpoint.getPath());
                    record.setControllerClass(endpoint.getControllerClass());
                    record.setControllerMethod(endpoint.getControllerMethod());
                    record.setFramework(endpoint.getFramework());
                    return record;
                })
                .toList();
        endpointRepository.saveAll(records);
    }
}
