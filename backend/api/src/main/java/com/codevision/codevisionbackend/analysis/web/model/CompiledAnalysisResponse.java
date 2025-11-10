package com.codevision.codevisionbackend.analysis.web.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class CompiledAnalysisResponse {

    private UUID analysisId;
    private String repoPath;
    private Instant startedAt;
    private Instant completedAt;
    private String status;
    private String statusMessage;
    private String outputDirectory;
    private Long entityCount;
    private Long endpointCount;
    private Long dependencyCount;
    private Long sequenceCount;
    private Long durationMillis;
    private List<ExportFileResponse> exports;

    public UUID getAnalysisId() {
        return analysisId;
    }

    public void setAnalysisId(UUID analysisId) {
        this.analysisId = analysisId;
    }

    public String getRepoPath() {
        return repoPath;
    }

    public void setRepoPath(String repoPath) {
        this.repoPath = repoPath;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    public String getOutputDirectory() {
        return outputDirectory;
    }

    public void setOutputDirectory(String outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    public Long getEntityCount() {
        return entityCount;
    }

    public void setEntityCount(Long entityCount) {
        this.entityCount = entityCount;
    }

    public Long getEndpointCount() {
        return endpointCount;
    }

    public void setEndpointCount(Long endpointCount) {
        this.endpointCount = endpointCount;
    }

    public Long getDependencyCount() {
        return dependencyCount;
    }

    public void setDependencyCount(Long dependencyCount) {
        this.dependencyCount = dependencyCount;
    }

    public Long getSequenceCount() {
        return sequenceCount;
    }

    public void setSequenceCount(Long sequenceCount) {
        this.sequenceCount = sequenceCount;
    }

    public Long getDurationMillis() {
        return durationMillis;
    }

    public void setDurationMillis(Long durationMillis) {
        this.durationMillis = durationMillis;
    }

    public List<ExportFileResponse> getExports() {
        return exports;
    }

    public void setExports(List<ExportFileResponse> exports) {
        this.exports = exports;
    }
}
