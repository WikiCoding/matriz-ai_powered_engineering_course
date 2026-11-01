package com.wikicoding.matrizbackend.projectmanagement.persistence.task;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface TaskRepository extends JpaRepository<TaskDataModel, UUID>, JpaSpecificationExecutor<TaskDataModel> {
}
