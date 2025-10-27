package com.codevision.codevisionbackend.project.db;

import com.codevision.codevisionbackend.project.Project;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DaoOperationRepository extends JpaRepository<DaoOperation, Long> {

    @Modifying
    @Query("delete from DaoOperation op where op.project = :project")
    void deleteByProject(@Param("project") Project project);

    List<DaoOperation> findByProjectId(Long projectId);
}

