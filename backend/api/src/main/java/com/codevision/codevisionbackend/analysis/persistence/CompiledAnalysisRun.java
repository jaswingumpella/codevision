package com.codevision.codevisionbackend.analysis.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "compiled_analysis_run")
public class CompiledAnalysisRun {

    @Id
    private UUID id;

    @Column(name = "repo_path", nullable = false)
    private String repoPath;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CompiledAnalysisRunStatus status;

    @Column(name = "status_message")
    private String statusMessage;

    @Column(name = "output_dir")
    private String outputDirectory;

    @Column(name = "entity_count")
    private Long entityCount;

    @Column(name = "endpoint_count")
    private Long endpointCount;

    @Column(name = "dependency_count")
    private Long dependencyCount;

    @Column(name = "sequence_count")
    private Long sequenceCount;

    @Column(name = "duration_ms")
    private Long durationMillis;

    @Column(name = "classpath")
    private String classpath;

    @Column(name = "accept_packages")
    private String acceptPackages;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public CompiledAnalysisRunStatus getStatus() {
        return status;
    }

    public void setStatus(CompiledAnalysisRunStatus status) {
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

    public String getClasspath() {
        return classpath;
    }

    public void setClasspath(String classpath) {
        this.classpath = classpath;
    }

    public String getAcceptPackages() {
        return acceptPackages;
    }

    public void setAcceptPackages(String acceptPackages) {
        this.acceptPackages = acceptPackages;
    }
}
