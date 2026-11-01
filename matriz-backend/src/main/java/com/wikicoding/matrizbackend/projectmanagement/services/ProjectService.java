package com.wikicoding.matrizbackend.projectmanagement.services;

import com.wikicoding.matrizbackend.projectmanagement.domain.project.Project;
import com.wikicoding.matrizbackend.projectmanagement.ProjectOwnerLookup;
import com.wikicoding.matrizbackend.projectmanagement.ProjectTeamLookup;
import com.wikicoding.matrizbackend.projectmanagement.persistence.project.ProjectDataModel;
import com.wikicoding.matrizbackend.projectmanagement.persistence.project.ProjectRepository;
import com.wikicoding.matrizbackend.projectmanagement.persistence.taskassignment.TaskAssignmentRepository;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ProjectService {
    private final ProjectRepository projectRepository;
    private final TaskAssignmentRepository taskAssignmentRepository;
    private final ProjectTeamLookup teamLookup;
    private final ProjectOwnerLookup ownerLookup;

    @Transactional(readOnly = true)
    public Page<Project> findAllProjects(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid pagination parameters");
        }
        return projectRepository.findAll(PageRequest.of(page, size, Sort.by(Sort.Order.asc("name"), Sort.Order.asc("id"))))
                .map(this::toDomain);
    }

    @Transactional(readOnly = true)
    public Project findProjectById(UUID projectId) {
        return projectRepository.findById(projectId)
                .map(this::toDomain)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
    }

    private Project toDomain(ProjectDataModel stored) {
        return new Project(stored.getId(), stored.getName(), stored.getDescription(), stored.getTeamId(), stored.getOwnerId());
    }

    @Transactional
    public Project createProject(String name, String description, UUID teamId, String subject) {
        Project validated = new Project(UUID.randomUUID(), name, description, validateExistingTeamByTeamId(teamId),
                ownerLookup.requireActiveOwnerId(subject));
        projectRepository.save(new ProjectDataModel(validated.getId(), validated.getName(), validated.getDescription(),
                validated.getTeamId(), validated.getOwnerId()));
        return validated;
    }

    @Transactional
    public Project updateProject(UUID projectId, String name, String description, UUID teamId) {
        ProjectDataModel stored = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));

        Project validated = new Project(projectId, name, description, validateExistingTeamByTeamId(teamId), stored.getOwnerId());
        stored.changeDetails(validated.getName(), validated.getDescription());

        boolean teamChanged = !stored.getTeamId().equals(teamId);

        if (teamChanged) {
            stored.assignTeamToProject(validated.getTeamId());
            taskAssignmentRepository.deleteInvalidForProject(projectId, teamId);
        }

        projectRepository.flush();

        return validated;
    }

    private UUID validateExistingTeamByTeamId(UUID teamId) {
        if (teamId == null || !teamLookup.exists(teamId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Team not found");
        }
        return teamId;
    }
}
