package com.codevision.codevisionbackend.project.metadata;

import com.codevision.codevisionbackend.project.Project;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClassMetadataRepository extends JpaRepository<ClassMetadata, Long> {

    @Modifying
    @Query("delete from ClassMetadata cm where cm.project = :project")
    void deleteByProject(@Param("project") Project project);

    List<ClassMetadata> findByProjectId(Long projectId);
}
