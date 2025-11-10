package com.codevision.codevisionbackend.analysis.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CompiledEndpointRepository extends JpaRepository<CompiledEndpointRecord, Long> {}
