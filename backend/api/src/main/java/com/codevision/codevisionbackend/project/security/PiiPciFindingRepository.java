package com.codevision.codevisionbackend.project.security;

import com.codevision.codevisionbackend.project.Project;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PiiPciFindingRepository extends JpaRepository<PiiPciFinding, Long> {

    @Modifying
    @Query("delete from PiiPciFinding finding where finding.project = :project")
    void deleteByProject(@Param("project") Project project);

    List<PiiPciFinding> findByProjectOrderBySeverityDescFilePathAsc(Project project);

    default List<PiiPciFinding> findByProject(Project project) {
        return findByProjectOrderBySeverityDescFilePathAsc(project);
    }

    Optional<PiiPciFinding> findByIdAndProjectId(Long id, Long projectId);
}
