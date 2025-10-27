package com.codevision.codevisionbackend.project.api;

import com.codevision.codevisionbackend.project.Project;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ApiEndpointRepository extends JpaRepository<ApiEndpoint, Long> {

    @Modifying
    @Query("delete from ApiEndpoint endpoint where endpoint.project = :project")
    void deleteByProject(@Param("project") Project project);

    List<ApiEndpoint> findByProjectId(Long projectId);
}

