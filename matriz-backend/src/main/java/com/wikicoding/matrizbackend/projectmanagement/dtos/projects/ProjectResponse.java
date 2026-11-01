package com.wikicoding.matrizbackend.projectmanagement.dtos.projects;

import com.wikicoding.matrizbackend.projectmanagement.domain.project.Project;
import java.util.UUID;

public record ProjectResponse(UUID id, String name, String description, UUID teamId, UUID ownerId) {
    public static ProjectResponse from(Project project) {
        return new ProjectResponse(project.getId(), project.getName(), project.getDescription(),
                project.getTeamId(), project.getOwnerId());
    }
}
