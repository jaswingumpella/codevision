package com.codevision.codevisionbackend.project;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    Optional<Project> findByRepoUrlAndBranchName(String repoUrl, String branchName);
    Optional<Project> findByProjectName(String projectName);
    Optional<Project> findByRepoUrl(String repoUrl);
    void deleteByRepoUrlAndBranchName(String repoUrl, String branchName);
}
