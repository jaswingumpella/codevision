package com.codevision.codevisionbackend.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import com.codevision.codevisionbackend.analysis.GraphModel.ClassNode;
import com.codevision.codevisionbackend.analysis.GraphModel.DependencyEdge;
import com.codevision.codevisionbackend.analysis.GraphModel.DependencyKind;
import com.codevision.codevisionbackend.analysis.GraphModel.EndpointNode;
import com.codevision.codevisionbackend.analysis.GraphModel.EndpointType;
import com.codevision.codevisionbackend.analysis.GraphModel.FieldModel;
import com.codevision.codevisionbackend.analysis.GraphModel.Origin;
import com.codevision.codevisionbackend.analysis.GraphModel.SequenceNode;
import com.codevision.codevisionbackend.analysis.GraphModel.SequenceUsage;
import org.junit.jupiter.api.Test;

class GraphMergerTest {

    private final GraphMerger merger = new GraphMerger();

    @Test
    void mergesSourceMetadataIntoBytecodeGraph() {
        GraphModel bytecodeModel = GraphModel.empty();
        ClassNode bytecodeNode = new ClassNode();
        bytecodeNode.setName("com.example.Fixture");
        bytecodeNode.setOrigin(Origin.BYTECODE);
        bytecodeNode.setTableName(null);
        bytecodeNode.getFields().add(field("id"));
        bytecodeModel.addClass(bytecodeNode);
        bytecodeModel.addDependency(new DependencyEdge(DependencyKind.CALL, "caller", "callee", "call"));
        bytecodeModel.addEndpoint(endpoint("com.example.Controller", "/path"));
        bytecodeModel.addSequence(sequence("seq"));

        GraphModel sourceModel = GraphModel.empty();
        ClassNode sourceNode = new ClassNode();
        sourceNode.setName("com.example.Fixture");
        sourceNode.setOrigin(Origin.SOURCE);
        sourceNode.setTableName("fixture_table");
        sourceNode.getFields().add(field("code"));
        sourceModel.addClass(sourceNode);
        sourceModel.addSequence(sequence("source-seq"));
        sourceModel.addSequenceUsage(new SequenceUsage("com.example.Fixture", "id", "source-seq"));

        GraphModel merged = merger.merge(sourceModel, bytecodeModel);

        ClassNode mergedNode = merged.getClasses().get("com.example.Fixture");
        assertThat(mergedNode.getOrigin()).isEqualTo(Origin.BOTH);
        assertThat(mergedNode.getTableName()).isEqualTo("fixture_table");
        assertThat(mergedNode.getFields()).extracting(FieldModel::getName).containsExactlyInAnyOrder("id", "code");

        assertThat(merged.getSequences()).containsKeys("seq", "source-seq");
        assertThat(merged.getSequenceUsages())
                .anySatisfy(usage -> assertThat(usage.getGeneratorName()).isEqualTo("source-seq"));
        assertThat(merged.getDependencyEdges()).anyMatch(edge -> edge.getKind() == DependencyKind.CALL);
        assertThat(merged.getEndpoints()).hasSize(1);
    }

    private FieldModel field(String name) {
        FieldModel model = new FieldModel();
        model.setName(name);
        model.setType("java.lang.String");
        return model;
    }

    private EndpointNode endpoint(String controller, String path) {
        EndpointNode node = new EndpointNode();
        node.setControllerClass(controller);
        node.setPath(path);
        node.setType(EndpointType.HTTP);
        return node;
    }

    private SequenceNode sequence(String name) {
        SequenceNode node = new SequenceNode();
        node.setGeneratorName(name);
        node.setSequenceName(name + "_seq");
        return node;
    }
}
