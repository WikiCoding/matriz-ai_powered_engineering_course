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
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProjectService {
    private final ProjectRepository projectRepository;
    private final TaskAssignmentRepository taskAssignmentRepository;
    private final ProjectTeamLookup teamLookup;
    private final ProjectOwnerLookup ownerLookup;

    @Transactional(readOnly = true)
    public Page<Project> findAllProjects(int page, int size) {
        log.debug("Operation started: findAllProjects");
        if (page < 0 || size < 1 || size > 100) {
            throw rejected(HttpStatus.BAD_REQUEST, "Invalid pagination parameters");
        }
        return projectRepository.findAll(PageRequest.of(page, size, Sort.by(Sort.Order.asc("name"), Sort.Order.asc("id"))))
                .map(this::toDomain);
    }

    @Transactional(readOnly = true)
    public Project findProjectById(UUID projectId) {
        log.debug("Operation started: findProjectById");
        return projectRepository.findById(projectId)
                .map(this::toDomain)
                .orElseThrow(() -> rejected(HttpStatus.NOT_FOUND, "Project not found"));
    }

    private Project toDomain(ProjectDataModel stored) {
        return new Project(stored.getId(), stored.getName(), stored.getDescription(), stored.getTeamId(), stored.getOwnerId());
    }

    @Transactional
    public Project createProject(String name, String description, UUID teamId, String subject) {
        log.debug("Operation started: createProject");
        Project validated = new Project(UUID.randomUUID(), name, description, validateExistingTeamByTeamId(teamId),
                ownerLookup.requireActiveOwnerId(subject));
        projectRepository.save(new ProjectDataModel(validated.getId(), validated.getName(), validated.getDescription(),
                validated.getTeamId(), validated.getOwnerId()));
        log.debug("Project write staged; awaiting transaction commit");
        return validated;
    }

    @Transactional
    public Project updateProject(UUID projectId, String name, String description, UUID teamId) {
        log.debug("Operation started: updateProject");
        ProjectDataModel stored = projectRepository.findById(projectId)
                .orElseThrow(() -> rejected(HttpStatus.NOT_FOUND, "Project not found"));

        Project validated = new Project(projectId, name, description, validateExistingTeamByTeamId(teamId), stored.getOwnerId());
        stored.changeDetails(validated.getName(), validated.getDescription());

        boolean teamChanged = !stored.getTeamId().equals(teamId);

        if (teamChanged) {
            stored.assignTeamToProject(validated.getTeamId());
            taskAssignmentRepository.deleteInvalidForProject(projectId, teamId);
            log.debug("Project team changed; invalid-assignment cleanup executed in current transaction");
        }

        projectRepository.flush();

        log.debug("Project write staged; awaiting transaction commit");
        return validated;
    }

    @Transactional
    public void deleteProject(UUID projectId) {
        log.debug("Operation started: deleteProject");
        ProjectDataModel project = projectRepository.findById(projectId)
                .orElseThrow(() -> rejected(HttpStatus.NOT_FOUND, "Project not found"));
        log.debug("Project deletion requested with task, comment and assignment cascades");
        projectRepository.delete(project);
        projectRepository.flush();
        log.debug("Project deletion flushed; awaiting transaction commit");
    }

    private UUID validateExistingTeamByTeamId(UUID teamId) {
        if (teamId == null || !teamLookup.exists(teamId)) {
            throw rejected(HttpStatus.BAD_REQUEST, "Team not found");
        }
        return teamId;
    }

    private ResponseStatusException rejected(HttpStatus status, String reason) {
        log.debug("Operation rejected status={} reason={}", status.value(), reason);
        return new ResponseStatusException(status, reason);
    }
}
