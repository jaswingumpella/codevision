package com.codevision.codevisionbackend.project;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    Optional<Project> findByRepoUrl(String repoUrl);
    void deleteByRepoUrl(String repoUrl);
}
