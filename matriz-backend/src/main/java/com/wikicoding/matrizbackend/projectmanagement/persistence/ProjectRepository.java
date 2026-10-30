package com.wikicoding.matrizbackend.projectmanagement.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProjectRepository extends JpaRepository<ProjectDataModel, UUID> {
}
