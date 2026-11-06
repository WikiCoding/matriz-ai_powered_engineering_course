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
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Slf4j
@RequiredArgsConstructor
public class TaskService {
    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final TaskAssignmentRepository taskAssignmentRepository;

    @Transactional(readOnly = true)
    public Page<Task> findAllTasks(UUID projectId, Status status, Priority priority, int page, int size) {
        log.debug("Operation started: findAllTasks");
        validateProjectExistsById(projectId);

        if (page < 0 || size < 1 || size > 100) {
            throw rejected(HttpStatus.BAD_REQUEST, "Invalid pagination parameters");
        }
        Specification<TaskDataModel> filter = (root, query, builder) ->
                builder.equal(root.get("projectId"), projectId);

        if (status != null) {
            filter = filter.and((root, query, builder) ->
                    builder.equal(root.get("status"), status));
        }
        if (priority != null) {
            filter = filter.and((root, query, builder) ->
                    builder.equal(root.get("priority"), priority));
        }
        return taskRepository.findAll(filter, PageRequest.of(page, size,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.asc("id")))).map(this::toDomain);
    }

    @Transactional(readOnly = true)
    public Task findTaskById(UUID id) {
        log.debug("Operation started: findTaskById");
        return toDomain(getTaskById(id));
    }

    @Transactional
    public Task createTask(TaskRequest request) {
        log.debug("Operation started: createTask");
        UUID projectId = request.projectId();

        validateProjectExistsById(projectId);

        Instant now = Instant.now();

        Task task = toTaskDomain(UUID.randomUUID(), projectId, request.title(), request.description(),
                request.effort(), request.status() == null ? Status.TODO : request.status(),
                request.priority() == null ? Priority.MEDIUM : request.priority(), now, now);

        taskRepository.save(new TaskDataModel(task.getId(), projectId, task.getTitle(), task.getDescription(),
                task.getEffort(), task.getStatus(), task.getPriority(), now, now, 0));

        log.debug("Task write staged status={} priority={}; awaiting transaction commit",
                task.getStatus(), task.getPriority());
        return task;
    }

    @Transactional
    public Task updateTask(UUID id, TaskUpdateRequest request) {
        log.debug("Operation started: updateTask");
        TaskDataModel stored = getTaskById(id);

        UUID projectId = request.projectId() == null ? stored.getProjectId() : request.projectId();

        var destination = projectRepository.findById(projectId)
                .orElseThrow(() -> rejected(HttpStatus.NOT_FOUND, "Project not found"));

        Instant now = Instant.now();

        Task task = toTaskDomain(id, projectId, request.title() == null ? stored.getTitle() : request.title(),
                request.description() == null ? stored.getDescription() : request.description(),
                request.effort() == null ? stored.getEffort() : request.effort(),
                request.status() == null ? stored.getStatus() : request.status(),
                request.priority() == null ? stored.getPriority() : request.priority(), stored.getCreatedAt(), now);

        if (!stored.getProjectId().equals(projectId)) {
            taskAssignmentRepository.deleteInvalidForTask(id, destination.getTeamId());
            log.debug("Task moved to another project; invalid-assignment cleanup executed in current transaction");
        }

        stored.changeDetails(projectId, task.getTitle(), task.getDescription(), task.getEffort(),
                task.getStatus(), task.getPriority(), now);
        taskRepository.flush();

        log.debug("Task write staged status={} priority={}; awaiting transaction commit",
                task.getStatus(), task.getPriority());
        return task;
    }

    @Transactional
    public void deleteTask(UUID id) {
        log.debug("Operation started: deleteTask");
        TaskDataModel task = getTaskById(id);
        log.debug("Task deletion requested with comment and assignment cascades");
        taskRepository.delete(task);
        taskRepository.flush();
        log.debug("Task deletion flushed; awaiting transaction commit");
    }

    private Task toTaskDomain(UUID id, UUID projectId, String title, String description, Short effort,
                              Status status, Priority priority, Instant createdAt, Instant updatedAt) {
        try {
            return new Task(id, projectId, title, description, effort == null ? 0 : effort,
                    status, priority, createdAt, updatedAt);
        } catch (IllegalArgumentException | NullPointerException exception) {
            log.debug("Domain validation rejected input exception={}", exception.getClass().getSimpleName());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
    }

    private Task toDomain(TaskDataModel stored) {
        return new Task(stored.getId(), stored.getProjectId(), stored.getTitle(), stored.getDescription(),
                stored.getEffort(), stored.getStatus(), stored.getPriority(), stored.getCreatedAt(), stored.getUpdatedAt());
    }

    private TaskDataModel getTaskById(UUID id) {
        return taskRepository.findById(id).orElseThrow(() -> rejected(HttpStatus.NOT_FOUND, "Task not found"));
    }

    private void validateProjectExistsById(UUID id) {
        if (id == null || !projectRepository.existsById(id)) {
            throw rejected(HttpStatus.NOT_FOUND, "Project not found");
        }
    }

    private ResponseStatusException rejected(HttpStatus status, String reason) {
        log.debug("Operation rejected status={} reason={}", status.value(), reason);
        return new ResponseStatusException(status, reason);
    }
}
