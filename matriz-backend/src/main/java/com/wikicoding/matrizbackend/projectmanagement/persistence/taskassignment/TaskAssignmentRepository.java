package com.wikicoding.matrizbackend.projectmanagement.persistence.taskassignment;

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
    @Query(value = "DELETE FROM task_assignment a USING app_user u WHERE a.user_id = u.id " +
            "AND a.task_id = :taskId AND u.team_id IS DISTINCT FROM :teamId", nativeQuery = true)
    void deleteInvalidForTask(@Param("taskId") UUID taskId, @Param("teamId") UUID teamId);

    @Modifying
    @Query(value = "DELETE FROM task_assignment a USING task t, app_user u WHERE a.task_id = t.id AND a.user_id = u.id " +
            "AND t.project_id = :projectId AND u.team_id IS DISTINCT FROM :teamId", nativeQuery = true)
    void deleteInvalidForProject(@Param("projectId") UUID projectId, @Param("teamId") UUID teamId);
}
