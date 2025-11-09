package com.codevision.codevisionbackend.project;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "project_snapshot")
@Getter
@Setter
@NoArgsConstructor
public class ProjectSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "project_id", nullable = false, insertable = false, updatable = false)
    private Long projectId;

    @Column(name = "project_name", nullable = false)
    private String projectName;

    @Column(name = "repo_url", nullable = false)
    private String repoUrl;

    @Column(name = "branch_name", nullable = false)
    private String branchName;

    @Column(name = "commit_hash")
    private String commitHash;

    @Column(name = "module_fingerprints_json", columnDefinition = "text")
    private String moduleFingerprintsJson;

    @Column(name = "snapshot_json", nullable = false, columnDefinition = "text")
    private String snapshotJson;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
}
