package com.codevision.codevisionbackend.project.security;

import com.codevision.codevisionbackend.project.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PiiPciFindingRepository extends JpaRepository<PiiPciFinding, Long> {

    @Modifying
    @Query("delete from PiiPciFinding finding where finding.project = :project")
    void deleteByProject(@Param("project") Project project);
}
