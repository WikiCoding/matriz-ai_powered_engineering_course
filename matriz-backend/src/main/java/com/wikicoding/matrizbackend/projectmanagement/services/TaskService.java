package com.wikicoding.matrizbackend.projectmanagement.services;

import com.wikicoding.matrizbackend.projectmanagement.domain.task.Status;
import com.wikicoding.matrizbackend.projectmanagement.domain.task.Priority;

import com.wikicoding.matrizbackend.projectmanagement.domain.task.Task;
import com.wikicoding.matrizbackend.projectmanagement.dtos.tasks.TaskRequest;
import com.wikicoding.matrizbackend.projectmanagement.dtos.tasks.TaskUpdateRequest;
import com.wikicoding.matrizbackend.projectmanagement.persistence.project.ProjectRepository;
import com.wikicoding.matrizbackend.projectmanagement.persistence.taskassignment.TaskAssignmentRepository;
import com.wikicoding.matrizbackend.projectmanagement.persistence.task.TaskDataModel;
import com.wikicoding.matrizbackend.projectmanagement.persistence.task.TaskRepository;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class TaskService {
    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final TaskAssignmentRepository taskAssignmentRepository;

    @Transactional(readOnly = true)
    public Page<Task> findAllTasks(UUID projectId, Status status, Priority priority, int page, int size) {
        requireProject(projectId);
        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid pagination parameters");
        }
        Specification<TaskDataModel> filter = (root, query, builder) -> builder.equal(root.get("projectId"), projectId);
        if (status != null) {
            filter = filter.and((root, query, builder) -> builder.equal(root.get("status"), status));
        }
        if (priority != null) {
            filter = filter.and((root, query, builder) -> builder.equal(root.get("priority"), priority));
        }
        return taskRepository.findAll(filter, PageRequest.of(page, size,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.asc("id")))).map(this::toDomain);
    }

    @Transactional(readOnly = true)
    public Task findTaskById(UUID id) {
        return toDomain(requireTask(id));
    }

    @Transactional
    public Task createTask(TaskRequest request) {
        UUID projectId = request.projectId();
        requireProject(projectId);
        Instant now = Instant.now();
        Task task = validated(UUID.randomUUID(), projectId, request.title(), request.description(),
                request.effort(), request.status() == null ? Status.TODO : request.status(),
                request.priority() == null ? Priority.MEDIUM : request.priority(), now, now);
        taskRepository.save(new TaskDataModel(task.getId(), projectId, task.getTitle(), task.getDescription(),
                task.getEffort(), task.getStatus(), task.getPriority(), now, now, 0));
        return task;
    }

    @Transactional
    public Task updateTask(UUID id, TaskUpdateRequest request) {
        TaskDataModel stored = requireTask(id);
        UUID projectId = request.projectId() == null ? stored.getProjectId() : request.projectId();
        var destination = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
        Instant now = Instant.now();
        Task task = validated(id, projectId, request.title() == null ? stored.getTitle() : request.title(),
                request.description() == null ? stored.getDescription() : request.description(),
                request.effort() == null ? stored.getEffort() : request.effort(),
                request.status() == null ? stored.getStatus() : request.status(),
                request.priority() == null ? stored.getPriority() : request.priority(), stored.getCreatedAt(), now);
        if (!stored.getProjectId().equals(projectId)) {
            taskAssignmentRepository.deleteInvalidForTask(id, destination.getTeamId());
        }
        stored.changeDetails(projectId, task.getTitle(), task.getDescription(), task.getEffort(),
                task.getStatus(), task.getPriority(), now);
        taskRepository.flush();
        return task;
    }

    private Task validated(UUID id, UUID projectId, String title, String description, Short effort,
                           Status status, Priority priority, Instant createdAt, Instant updatedAt) {
        try {
            return new Task(id, projectId, title, description, effort == null ? 0 : effort,
                    status, priority, createdAt, updatedAt);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
    }

    private Task toDomain(TaskDataModel stored) {
        return new Task(stored.getId(), stored.getProjectId(), stored.getTitle(), stored.getDescription(),
                stored.getEffort(), stored.getStatus(), stored.getPriority(), stored.getCreatedAt(), stored.getUpdatedAt());
    }

    private TaskDataModel requireTask(UUID id) {
        return taskRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));
    }

    private void requireProject(UUID id) {
        if (id == null || !projectRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found");
        }
    }
}
