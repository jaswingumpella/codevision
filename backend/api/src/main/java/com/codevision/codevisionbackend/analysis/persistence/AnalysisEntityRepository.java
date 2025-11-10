package com.codevision.codevisionbackend.analysis.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnalysisEntityRepository extends JpaRepository<AnalysisEntityRecord, Long> {
    AnalysisEntityRecord findByClassName(String className);

    Page<AnalysisEntityRecord> findByPackageNameStartingWithIgnoreCase(String packageName, Pageable pageable);
}
