package com.wikicoding.matrizbackend.projectmanagement.persistence.task;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import com.wikicoding.matrizbackend.projectmanagement.domain.task.Status;
import java.util.List;

import java.util.UUID;

public interface TaskRepository extends JpaRepository<TaskDataModel, UUID>, JpaSpecificationExecutor<TaskDataModel> {
    @Query("select t.status as status, count(t) as total from TaskDataModel t group by t.status")
    List<StatusCount> countByStatus();

    interface StatusCount {
        Status getStatus();
        long getTotal();
    }
}
