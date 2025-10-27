package com.codevision.codevisionbackend.project.asset;

import com.codevision.codevisionbackend.project.Project;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AssetImageRepository extends JpaRepository<AssetImage, Long> {

    @Modifying
    @Query("delete from AssetImage asset where asset.project = :project")
    void deleteByProject(@Param("project") Project project);

    List<AssetImage> findByProjectId(Long projectId);
}

