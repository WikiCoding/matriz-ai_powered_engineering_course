package com.wikicoding.matrizbackend.projectmanagement.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskAssignmentRepository extends JpaRepository<TaskAssignmentDataModel, UUID> {
    void deleteByUserId(UUID userId);

    @Modifying
    @Query(value = "DELETE FROM task_assignment a USING task t, app_user u WHERE a.task_id = t.id AND a.user_id = u.id " +
            "AND t.project_id = :projectId AND u.team_id IS DISTINCT FROM :teamId", nativeQuery = true)
    void deleteInvalidForProject(@Param("projectId") UUID projectId, @Param("teamId") UUID teamId);
}
