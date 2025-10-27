package com.codevision.codevisionbackend.project;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    Optional<Project> findByRepoUrl(String repoUrl);
    Optional<Project> findByProjectName(String projectName);
    void deleteByRepoUrl(String repoUrl);
}
