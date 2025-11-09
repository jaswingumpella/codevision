package com.codevision.codevisionbackend.project;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.OffsetDateTime;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "project", uniqueConstraints = @UniqueConstraint(name = "uq_project_repo_branch", columnNames = {"repo_url", "branch_name"}))
@Data
@NoArgsConstructor
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "repo_url", nullable = false)
    private String repoUrl;

    @Column(name = "project_name", nullable = false)
    private String projectName;

    @Column(name = "branch_name", nullable = false)
    private String branchName;

    @Column(name = "last_analyzed_at", nullable = false)
    private OffsetDateTime lastAnalyzedAt;

    @Column(name = "build_group_id")
    private String buildGroupId;

    @Column(name = "build_artifact_id")
    private String buildArtifactId;

    @Column(name = "build_version")
    private String buildVersion;

    @Column(name = "build_java_version")
    private String buildJavaVersion;

    public Project(String repoUrl, String projectName, String branchName, OffsetDateTime lastAnalyzedAt) {
        this.repoUrl = repoUrl;
        this.projectName = projectName;
        this.branchName = branchName;
        this.lastAnalyzedAt = lastAnalyzedAt;
    }
}
