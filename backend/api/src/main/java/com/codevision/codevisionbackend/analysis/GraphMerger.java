package com.codevision.codevisionbackend.analysis;

import com.codevision.codevisionbackend.analysis.GraphModel.ClassNode;
import com.codevision.codevisionbackend.analysis.GraphModel.DependencyEdge;
import com.codevision.codevisionbackend.analysis.GraphModel.EndpointNode;
import com.codevision.codevisionbackend.analysis.GraphModel.FieldModel;
import com.codevision.codevisionbackend.analysis.GraphModel.Origin;
import com.codevision.codevisionbackend.analysis.GraphModel.SequenceNode;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Merges source-derived metadata with bytecode-derived metadata so downstream writers operate on a
 * single canonical graph.
 */
@Component
public class GraphMerger {

    public GraphModel merge(GraphModel sourceModel, GraphModel bytecodeModel) {
        GraphModel merged = GraphModel.empty();
        if (bytecodeModel != null) {
            bytecodeModel.getClasses().values().forEach(node -> merged.addClass(node.copy()));
            bytecodeModel.getSequences().values().forEach(merged::addSequence);
            bytecodeModel.getSequenceUsages().forEach(merged::addSequenceUsage);
            bytecodeModel.getEndpoints().forEach(merged::addEndpoint);
            bytecodeModel.getDependencyEdges().forEach(merged::addDependency);
            bytecodeModel.getMethodCallEdges().forEach(merged::addMethodCallEdge);
        }

        if (sourceModel != null) {
            mergeClassesFromSource(sourceModel.getClasses(), merged.getClasses());
            sourceModel.getSequences().values().forEach(merged::addSequence);
            sourceModel.getSequenceUsages().forEach(merged::addSequenceUsage);
            sourceModel.getEndpoints().forEach(merged::addEndpoint);
            sourceModel.getDependencyEdges().forEach(merged::addDependency);
        }

        return merged;
    }

    private void mergeClassesFromSource(Map<String, ClassNode> sourceClasses, Map<String, ClassNode> targetClasses) {
        for (ClassNode sourceNode : sourceClasses.values()) {
            ClassNode target = targetClasses.get(sourceNode.getName());
            if (target == null) {
                ClassNode copy = sourceNode.copy();
                copy.setOrigin(Origin.SOURCE);
                targetClasses.put(copy.getName(), copy);
                continue;
            }
            target.setOrigin(Origin.BOTH);
            if (target.getTableName() == null) {
                target.setTableName(sourceNode.getTableName());
            }
            mergeLists(target.getAnnotations(), sourceNode.getAnnotations());
            mergeLists(target.getStereotypes(), sourceNode.getStereotypes());
            mergeFields(target, sourceNode);
        }
    }

    private void mergeFields(ClassNode target, ClassNode source) {
        Set<String> existingFieldNames = target.getFields().stream()
                .map(FieldModel::getName)
                .collect(LinkedHashSet::new, Set::add, Set::addAll);
        for (FieldModel fieldModel : source.getFields()) {
            if (fieldModel == null || fieldModel.getName() == null) {
                continue;
            }
            if (!existingFieldNames.contains(fieldModel.getName())) {
                target.getFields().add(fieldModel.copy());
                existingFieldNames.add(fieldModel.getName());
            }
        }
    }

    private void mergeLists(List<String> target, List<String> source) {
        Set<String> seen = new LinkedHashSet<>(target);
        for (String entry : source) {
            if (entry != null && !seen.contains(entry)) {
                target.add(entry);
                seen.add(entry);
            }
        }
    }
}
