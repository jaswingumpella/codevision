package com.codevision.codevisionbackend.analyze.job;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AnalysisJobRepository extends JpaRepository<AnalysisJob, UUID> {}
