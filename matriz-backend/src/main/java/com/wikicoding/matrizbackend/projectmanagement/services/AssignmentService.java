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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AssignmentService {
    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final TaskAssignmentRepository assignmentRepository;
    private final UserEligibility userEligibility;

    @Transactional(readOnly = true)
    public List<Assignment> list(UUID taskId) {
        if (!taskRepository.existsById(taskId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found");
        }

        return assignmentRepository.findById(taskId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public Assignment add(UUID taskId, UUID userId) {
        var task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));

        var project = projectRepository.findById(task.getProjectId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));

        if (assignmentRepository.existsById(taskId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Task already has an assignee");
        }

        userEligibility.requireSoftwareEngineerInTeam(userId, project.getTeamId());

        try {
            assignmentRepository.saveAndFlush(new TaskAssignmentDataModel(taskId, userId));
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Task already has an assignee", exception);
        }
        return new Assignment(taskId, userId);
    }

    @Transactional
    public void remove(UUID taskId, UUID userId) {
        if (!taskRepository.existsById(taskId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found");
        }

        var assignment = assignmentRepository.findById(taskId)
                .filter(stored -> stored.getUserId().equals(userId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found"));

        assignmentRepository.delete(assignment);
        assignmentRepository.flush();
    }

    private Assignment toResponse(TaskAssignmentDataModel stored) {
        return new Assignment(stored.getTaskId(), stored.getUserId());
    }

    public record Assignment(UUID taskId, UUID userId) {}
}
