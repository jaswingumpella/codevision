package com.codevision.codevisionbackend.analysis.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompiledAnalysisRunRepository extends JpaRepository<CompiledAnalysisRun, UUID> {
    Optional<CompiledAnalysisRun> findTopByOrderByStartedAtDesc();

    Optional<CompiledAnalysisRun> findTopByProjectIdOrderByStartedAtDesc(Long projectId);
}
