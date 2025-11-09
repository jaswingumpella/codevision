package com.codevision.codevisionbackend.project;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Persistable;

@Entity
@Table(name = "project_snapshot")
@Getter
@NoArgsConstructor
public class ProjectSnapshot implements Persistable<Long> {

    @Id
    @Column(name = "project_id")
    private Long projectId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", insertable = false, updatable = false)
    private Project project;

    @Setter
    @Column(name = "project_name", nullable = false)
    private String projectName;

    @Setter
    @Column(name = "repo_url", nullable = false)
    private String repoUrl;

    @Setter
    @Column(name = "snapshot_json", nullable = false, columnDefinition = "text")
    private String snapshotJson;

    @Setter
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Transient
    private boolean newSnapshot = true;

    public ProjectSnapshot(Project project, String snapshotJson, OffsetDateTime createdAt) {
        setProject(project);
        this.snapshotJson = snapshotJson;
        this.createdAt = createdAt;
    }

    public void setProject(Project project) {
        if (project == null) {
            this.project = null;
            this.projectId = null;
            return;
        }
        if (project.getId() == null) {
            throw new IllegalArgumentException("Project must have an identifier before attaching to snapshot");
        }
        this.project = project;
        this.projectId = project.getId();
    }

    @Override
    public Long getId() {
        return projectId;
    }

    @Override
    public boolean isNew() {
        return newSnapshot;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.newSnapshot = false;
    }
}
