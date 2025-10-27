package com.codevision.codevisionbackend.project;

import com.codevision.codevisionbackend.analyze.BuildInfo;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;

    public ProjectService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    @Transactional
    public Project overwriteProject(String repoUrl, String projectName, BuildInfo buildInfo) {
        Project project = projectRepository.findByRepoUrl(repoUrl).orElseGet(Project::new);
        project.setRepoUrl(repoUrl);
        project.setProjectName(projectName);
        project.setLastAnalyzedAt(OffsetDateTime.now());
        applyBuildInfo(project, buildInfo);
        return projectRepository.saveAndFlush(project);
    }

    @Transactional(readOnly = true)
    public Optional<Project> findByRepoUrl(String repoUrl) {
        return projectRepository.findByRepoUrl(repoUrl);
    }

    @Transactional(readOnly = true)
    public Optional<Project> findByProjectName(String projectName) {
        return projectRepository.findByProjectName(projectName);
    }

    @Transactional
    public void delete(Project project) {
        projectRepository.delete(project);
        projectRepository.flush();
    }

    private void applyBuildInfo(Project project, BuildInfo buildInfo) {
        if (buildInfo == null) {
            project.setBuildGroupId(null);
            project.setBuildArtifactId(null);
            project.setBuildVersion(null);
            project.setBuildJavaVersion(null);
            return;
        }
        project.setBuildGroupId(buildInfo.groupId());
        project.setBuildArtifactId(buildInfo.artifactId());
        project.setBuildVersion(buildInfo.version());
        project.setBuildJavaVersion(buildInfo.javaVersion());
    }
}
