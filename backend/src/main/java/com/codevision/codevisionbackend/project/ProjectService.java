package com.codevision.codevisionbackend.project;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;

    public ProjectService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    @Transactional
    public Project overwriteProject(String repoUrl, String projectName) {
        projectRepository.deleteByRepoUrl(repoUrl);
        Project project = new Project(repoUrl, projectName, OffsetDateTime.now());
        return projectRepository.save(project);
    }
}
