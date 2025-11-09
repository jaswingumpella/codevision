package com.codevision.codevisionbackend.analyze.job;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "analysis_job")
@Getter
@Setter
@NoArgsConstructor
public class AnalysisJob {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "repo_url", nullable = false, length = 2048)
    private String repoUrl;

    @Column(name = "branch_name", nullable = false, length = 255)
    private String branchName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private AnalysisJobStatus status;

    @Column(name = "status_message", length = 512)
    private String statusMessage;

    @Column(name = "project_id")
    private Long projectId;

    @Column(name = "error_message", length = 1024)
    private String errorMessage;

    @Column(name = "commit_hash", length = 96)
    private String commitHash;

    @Column(name = "snapshot_id")
    private Long snapshotId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;
}
