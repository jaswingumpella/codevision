package com.codevision.codevisionbackend.analysis.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SequenceRecordRepository extends JpaRepository<SequenceRecord, Long> {
    Optional<SequenceRecord> findByGeneratorName(String generatorName);
}
