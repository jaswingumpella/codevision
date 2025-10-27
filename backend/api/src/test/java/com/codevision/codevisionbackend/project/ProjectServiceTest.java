package com.codevision.codevisionbackend.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codevision.codevisionbackend.analyze.BuildInfo;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    private ProjectService projectService;

    @BeforeEach
    void setUp() {
        projectService = new ProjectService(projectRepository);
    }

    @Test
    void overwriteProjectDeletesExistingAndSavesNew() {
        BuildInfo buildInfo = new BuildInfo("com.barclays", "codevision", "1.0.0", "21");
        Project savedProject = new Project("https://example.com/repo.git", "codevision", OffsetDateTime.now());
        savedProject.setId(7L);
        when(projectRepository.findByRepoUrl("https://example.com/repo.git")).thenReturn(Optional.empty());
        when(projectRepository.saveAndFlush(argThat(project -> project.getRepoUrl().equals("https://example.com/repo.git"))))
                .thenReturn(savedProject);

        Project result = projectService.overwriteProject("https://example.com/repo.git", "codevision", buildInfo);

        verify(projectRepository).findByRepoUrl("https://example.com/repo.git");
        verify(projectRepository).saveAndFlush(argThat(project -> {
            assertEquals("com.barclays", project.getBuildGroupId());
            assertEquals("codevision", project.getProjectName());
            assertEquals("codevision", project.getBuildArtifactId());
            assertEquals("1.0.0", project.getBuildVersion());
            assertEquals("21", project.getBuildJavaVersion());
            assertNotNull(project.getLastAnalyzedAt());
            return true;
        }));

        assertEquals(savedProject, result);
    }

    @Test
    void overwriteProjectUpdatesExistingRecord() {
        BuildInfo buildInfo = new BuildInfo("com.barclays", "codevision", "1.0.1", "21");
        Project existing = new Project("https://example.com/repo.git", "old-name", OffsetDateTime.now().minusDays(1));
        existing.setId(9L);

        when(projectRepository.findByRepoUrl("https://example.com/repo.git")).thenReturn(Optional.of(existing));
        when(projectRepository.saveAndFlush(existing)).thenReturn(existing);

        Project result = projectService.overwriteProject("https://example.com/repo.git", "codevision", buildInfo);

        assertEquals(existing, result);
        assertEquals("codevision", existing.getProjectName());
        assertEquals("1.0.1", existing.getBuildVersion());
        assertEquals("codevision", existing.getBuildArtifactId());
        assertEquals("com.barclays", existing.getBuildGroupId());
        assertNotNull(existing.getLastAnalyzedAt());
        verify(projectRepository).saveAndFlush(existing);
    }
}
