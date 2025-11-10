package com.codevision.codevisionbackend.analysis.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EntitySequenceUsageRepository
        extends JpaRepository<EntitySequenceUsageRecord, EntitySequenceUsageId> {}
