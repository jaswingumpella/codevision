package com.codevision.codevisionbackend.project;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectSnapshotRepository extends JpaRepository<ProjectSnapshot, Long> {

    Optional<ProjectSnapshot> findTopByProjectIdOrderByCreatedAtDesc(Long projectId);

    Optional<ProjectSnapshot> findTopByProjectIdAndCommitHashOrderByCreatedAtDesc(Long projectId, String commitHash);

    List<ProjectSnapshot> findByProjectIdOrderByCreatedAtDesc(Long projectId);

    Optional<ProjectSnapshot> findByIdAndProjectId(Long snapshotId, Long projectId);
}
