package com.codevision.codevisionbackend.project;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "project")
@Data
@NoArgsConstructor
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "repo_url", nullable = false, unique = true)
    private String repoUrl;

    @Column(name = "project_name", nullable = false)
    private String projectName;

    @Column(name = "last_analyzed_at", nullable = false)
    private OffsetDateTime lastAnalyzedAt;

    public Project(String repoUrl, String projectName, OffsetDateTime lastAnalyzedAt) {
        this.repoUrl = repoUrl;
        this.projectName = projectName;
        this.lastAnalyzedAt = lastAnalyzedAt;
    }
}
