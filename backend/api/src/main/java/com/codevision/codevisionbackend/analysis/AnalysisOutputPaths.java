package com.codevision.codevisionbackend.analysis;

import java.nio.file.Path;
import java.util.List;

/**
 * Aggregates the filesystem locations of generated analysis artifacts.
 */
public class AnalysisOutputPaths {

    private final Path rootDirectory;
    private final Path analysisJson;
    private final Path entitiesCsv;
    private final Path sequencesCsv;
    private final Path endpointsCsv;
    private final Path dependenciesCsv;
    private final DiagramWriter.DiagramArtifacts diagramArtifacts;

    public AnalysisOutputPaths(
            Path rootDirectory,
            Path analysisJson,
            Path entitiesCsv,
            Path sequencesCsv,
            Path endpointsCsv,
            Path dependenciesCsv,
            DiagramWriter.DiagramArtifacts diagramArtifacts) {
        this.rootDirectory = rootDirectory;
        this.analysisJson = analysisJson;
        this.entitiesCsv = entitiesCsv;
        this.sequencesCsv = sequencesCsv;
        this.endpointsCsv = endpointsCsv;
        this.dependenciesCsv = dependenciesCsv;
        this.diagramArtifacts = diagramArtifacts;
    }

    public Path getRootDirectory() {
        return rootDirectory;
    }

    public Path getAnalysisJson() {
        return analysisJson;
    }

    public Path getEntitiesCsv() {
        return entitiesCsv;
    }

    public Path getSequencesCsv() {
        return sequencesCsv;
    }

    public Path getEndpointsCsv() {
        return endpointsCsv;
    }

    public Path getDependenciesCsv() {
        return dependenciesCsv;
    }

    public DiagramWriter.DiagramArtifacts getDiagramArtifacts() {
        return diagramArtifacts;
    }

    public List<Path> allFiles() {
        List<Path> files = new java.util.ArrayList<>(List.of(
                analysisJson,
                entitiesCsv,
                sequencesCsv,
                endpointsCsv,
                dependenciesCsv,
                diagramArtifacts.classDiagram(),
                diagramArtifacts.erdPlantUml(),
                diagramArtifacts.erdMermaid()));
        files.addAll(diagramArtifacts.sequenceDiagrams());
        return files;
    }
}
