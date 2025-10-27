package com.codevision.codevisionbackend.project;

import com.codevision.codevisionbackend.analyze.ParsedDataResponse;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Utility for local debugging")
class ProjectSnapshotDumpTest {

    @Autowired
    private ProjectSnapshotRepository projectSnapshotRepository;

    @Autowired
    private ProjectSnapshotService projectSnapshotService;

    @Test
    void dumpLatestSnapshot() {
        projectSnapshotRepository.findAll().stream()
                .reduce((first, second) -> second)
                .map(ProjectSnapshot::getProjectId)
                .flatMap(projectSnapshotService::fetchSnapshot)
                .ifPresentOrElse(this::logSnapshot, () -> System.out.println("No snapshots found"));
    }

    private void logSnapshot(ParsedDataResponse snapshot) {
        System.out.printf(
                "Project %s (%d) repo=%s classes=%d analyzedAt=%s%n",
                snapshot.projectName(),
                snapshot.projectId(),
                snapshot.repoUrl(),
                snapshot.classes().size(),
                snapshot.analyzedAt());
    }
}
