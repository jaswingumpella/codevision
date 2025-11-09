package com.codevision.codevisionbackend.project;

import com.codevision.codevisionbackend.analyze.BuildInfo;
import static com.codevision.codevisionbackend.git.BranchUtils.normalize;

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
    public Project overwriteProject(String repoUrl, String branchName, String projectName, BuildInfo buildInfo) {
        String normalizedBranch = normalize(branchName);
        Project project = projectRepository
                .findByRepoUrlAndBranchName(repoUrl, normalizedBranch)
                .orElseGet(Project::new);
        project.setRepoUrl(repoUrl);
        project.setBranchName(normalizedBranch);
        project.setProjectName(projectName);
        project.setLastAnalyzedAt(OffsetDateTime.now());
        applyBuildInfo(project, buildInfo);
        return projectRepository.saveAndFlush(project);
    }

    @Transactional(readOnly = true)
    public Optional<Project> findByRepoUrlAndBranch(String repoUrl, String branchName) {
        return projectRepository.findByRepoUrlAndBranchName(repoUrl, normalize(branchName));
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
