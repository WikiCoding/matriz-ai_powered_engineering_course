package com.wikicoding.matrizbackend.projectmanagement.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TaskAssignmentRepository extends JpaRepository<TaskAssignmentDataModel, UUID> {
}
