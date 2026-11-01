package com.wikicoding.matrizbackend.projectmanagement.controllers;

import com.wikicoding.matrizbackend.projectmanagement.dtos.projects.ProjectResponse;
import com.wikicoding.matrizbackend.projectmanagement.dtos.projects.ProjectPageResponse;
import com.wikicoding.matrizbackend.projectmanagement.dtos.projects.ProjectRequest;
import com.wikicoding.matrizbackend.projectmanagement.services.ProjectService;
import com.wikicoding.matrizbackend.projectmanagement.services.TaskService;
import com.wikicoding.matrizbackend.projectmanagement.domain.task.Status;
import com.wikicoding.matrizbackend.projectmanagement.domain.task.Priority;
import com.wikicoding.matrizbackend.projectmanagement.dtos.tasks.TaskPageResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectController {
    private final ProjectService projectService;
    private final TaskService taskService;

    @GetMapping
    public ProjectPageResponse findAll(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ProjectPageResponse.from(projectService.findAllProjects(page, size));
    }

    @GetMapping("/{projectId}")
    public ProjectResponse findById(@PathVariable UUID projectId) {
        return ProjectResponse.from(projectService.findProjectById(projectId));
    }

    @PostMapping
    public ResponseEntity<ProjectResponse> create(@Valid @RequestBody ProjectRequest request,
            JwtAuthenticationToken principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ProjectResponse.from(
                projectService.createProject(request.name(), request.description(), request.teamId(), principal.getName())));
    }

    @PutMapping("/{projectId}")
    public ProjectResponse update(@PathVariable UUID projectId, @Valid @RequestBody ProjectRequest request) {
        return ProjectResponse.from(projectService.updateProject(projectId, request.name(), request.description(), request.teamId()));
    }

    @GetMapping("/{projectId}/tasks")
    public TaskPageResponse findAllTasksFromProject(@PathVariable UUID projectId,
            @RequestParam(required = false) Status status, @RequestParam(required = false) Priority priority,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return TaskPageResponse.from(taskService.findAllTasks(projectId, status, priority, page, size));
    }

}
