package com.wikicoding.matrizbackend.projectmanagement.services;

import com.wikicoding.matrizbackend.projectmanagement.persistence.project.ProjectRepository;
import com.wikicoding.matrizbackend.projectmanagement.persistence.task.TaskRepository;
import com.wikicoding.matrizbackend.projectmanagement.persistence.taskassignment.TaskAssignmentDataModel;
import com.wikicoding.matrizbackend.projectmanagement.persistence.taskassignment.TaskAssignmentRepository;
import com.wikicoding.matrizbackend.projectmanagement.UserEligibility;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Slf4j
@RequiredArgsConstructor
public class AssignmentService {
    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final TaskAssignmentRepository assignmentRepository;
    private final UserEligibility userEligibility;

    @Transactional(readOnly = true)
    public List<Assignment> list(UUID taskId) {
        log.debug("Operation started: list");
        if (!taskRepository.existsById(taskId)) {
            throw rejected(HttpStatus.NOT_FOUND, "Task not found");
        }

        return assignmentRepository.findById(taskId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public Assignment add(UUID taskId, UUID userId) {
        log.debug("Operation started: add");
        var task = taskRepository.findById(taskId)
                .orElseThrow(() -> rejected(HttpStatus.NOT_FOUND, "Task not found"));

        var project = projectRepository.findById(task.getProjectId())
                .orElseThrow(() -> rejected(HttpStatus.NOT_FOUND, "Project not found"));

        if (assignmentRepository.existsById(taskId)) {
            throw rejected(HttpStatus.CONFLICT, "Task already has an assignee");
        }

        try {
            userEligibility.requireSoftwareEngineerInTeam(userId, project.getTeamId());
        } catch (RuntimeException exception) {
            log.debug("Assignment eligibility check failed exception={}", exception.getClass().getSimpleName());
            throw exception;
        }
        log.debug("Assignment eligibility check passed");

        try {
            assignmentRepository.saveAndFlush(new TaskAssignmentDataModel(taskId, userId));
        } catch (DataIntegrityViolationException exception) {
            log.debug("Assignment creation rejected by persistence constraint");
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Task already has an assignee", exception);
        }
        log.debug("Assignment creation flushed; awaiting transaction commit");
        return new Assignment(taskId, userId);
    }

    @Transactional
    public void remove(UUID taskId, UUID userId) {
        log.debug("Operation started: remove");
        if (!taskRepository.existsById(taskId)) {
            throw rejected(HttpStatus.NOT_FOUND, "Task not found");
        }

        var assignment = assignmentRepository.findById(taskId)
                .filter(stored -> stored.getUserId().equals(userId))
                .orElseThrow(() -> rejected(HttpStatus.NOT_FOUND, "Assignment not found"));

        assignmentRepository.delete(assignment);
        assignmentRepository.flush();
        log.debug("Assignment removal flushed; awaiting transaction commit");
    }

    private Assignment toResponse(TaskAssignmentDataModel stored) {
        return new Assignment(stored.getTaskId(), stored.getUserId());
    }

    public record Assignment(UUID taskId, UUID userId) {}

    private ResponseStatusException rejected(HttpStatus status, String reason) {
        log.debug("Operation rejected status={} reason={}", status.value(), reason);
        return new ResponseStatusException(status, reason);
    }
}
