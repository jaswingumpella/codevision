package com.codevision.codevisionbackend.project.diagram;

import com.codevision.codevisionbackend.project.Project;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DiagramRepository extends JpaRepository<Diagram, Long> {

    List<Diagram> findByProjectIdOrderByDiagramTypeAscSequenceOrderAscTitleAsc(Long projectId);

    Optional<Diagram> findByProjectIdAndId(Long projectId, Long diagramId);

    @Modifying
    @Query("delete from Diagram d where d.project = :project")
    void deleteByProject(@Param("project") Project project);
}
