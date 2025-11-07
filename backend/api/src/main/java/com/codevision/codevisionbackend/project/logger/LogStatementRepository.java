package com.codevision.codevisionbackend.project.logger;

import com.codevision.codevisionbackend.project.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LogStatementRepository extends JpaRepository<LogStatement, Long> {

    @Modifying
    @Query("delete from LogStatement ls where ls.project = :project")
    void deleteByProject(@Param("project") Project project);
}
