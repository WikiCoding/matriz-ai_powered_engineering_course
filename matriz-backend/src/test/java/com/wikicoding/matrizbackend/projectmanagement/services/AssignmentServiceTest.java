package com.wikicoding.matrizbackend.projectmanagement.services;

import com.wikicoding.matrizbackend.projectmanagement.persistence.project.ProjectDataModel;
import com.wikicoding.matrizbackend.projectmanagement.persistence.project.ProjectRepository;
import com.wikicoding.matrizbackend.projectmanagement.persistence.task.TaskDataModel;
import com.wikicoding.matrizbackend.projectmanagement.persistence.task.TaskRepository;
import com.wikicoding.matrizbackend.projectmanagement.persistence.taskassignment.TaskAssignmentDataModel;
import com.wikicoding.matrizbackend.projectmanagement.persistence.taskassignment.TaskAssignmentRepository;
import com.wikicoding.matrizbackend.user.UserEligibility;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AssignmentServiceTest {
    private final TaskRepository tasks = mock(TaskRepository.class);
    private final ProjectRepository projects = mock(ProjectRepository.class);
    private final TaskAssignmentRepository assignments = mock(TaskAssignmentRepository.class);
    private final UserEligibility eligibility = mock(UserEligibility.class);
    private final AssignmentService service = new AssignmentService(tasks, projects, assignments, eligibility);
    private final UUID taskId = UUID.randomUUID();
    private final UUID projectId = UUID.randomUUID();
    private final UUID teamId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    private void existingTask() {
        TaskDataModel task = mock(TaskDataModel.class);
        when(task.getProjectId()).thenReturn(projectId);
        when(tasks.findById(taskId)).thenReturn(Optional.of(task));
    }

    @Test
    void addsEligibleEngineerToUnassignedTask() {
        // Arrange
        existingTask();
        when(projects.findById(projectId)).thenReturn(Optional.of(
                new ProjectDataModel(projectId, "Project", "", teamId, UUID.randomUUID())));

        // Act
        var result = service.add(taskId, userId);

        // Assert
        assertThat(result.userId()).isEqualTo(userId);
        verify(eligibility).requireSoftwareEngineerInTeam(userId, teamId);
        verify(assignments).saveAndFlush(any(TaskAssignmentDataModel.class));
    }

    @Test
    void occupiedTaskRejectsAnotherAssignmentWithoutWrite() {
        // Arrange
        existingTask();
        when(projects.findById(projectId)).thenReturn(Optional.of(
                new ProjectDataModel(projectId, "Project", "", teamId, UUID.randomUUID())));
        when(assignments.existsById(taskId)).thenReturn(true);

        // Act / Assert
        assertThatThrownBy(() -> service.add(taskId, userId))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        error -> assertThat(error.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
        verify(assignments, never()).saveAndFlush(any());
        verifyNoInteractions(eligibility);
    }

    @Test
    void ineligibleUserCannotBeAssigned() {
        // Arrange
        existingTask();
        when(projects.findById(projectId)).thenReturn(Optional.of(
                new ProjectDataModel(projectId, "Project", "", teamId, UUID.randomUUID())));
        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ineligible assignee"))
                .when(eligibility).requireSoftwareEngineerInTeam(userId, teamId);

        // Act / Assert
        assertThatThrownBy(() -> service.add(taskId, userId))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        error -> assertThat(error.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        verify(assignments, never()).saveAndFlush(any());
    }

    @Test
    void missingTaskRejectsAssignmentWithoutWrite() {
        // Arrange
        // Act / Assert
        assertThatThrownBy(() -> service.add(taskId, userId))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        error -> assertThat(error.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        verifyNoInteractions(assignments, eligibility);
    }

    @Test
    void listsAssignmentForExistingTask() {
        // Arrange
        existingTask();
        var stored = new TaskAssignmentDataModel(taskId, userId);
        when(assignments.findById(taskId)).thenReturn(Optional.of(stored));

        // Act
        var listed = service.list(taskId);

        // Assert
        assertThat(listed).containsExactly(new AssignmentService.Assignment(taskId, userId));
    }

    @Test
    void removesOnlyMatchingAssignment() {
        // Arrange
        existingTask();
        var stored = new TaskAssignmentDataModel(taskId, userId);
        when(assignments.findById(taskId)).thenReturn(Optional.of(stored));

        // Act
        service.remove(taskId, userId);

        // Assert
        verify(assignments).delete(stored);
        verify(assignments).flush();
    }

    @Test
    void wrongUserCannotRemoveAssignment() {
        // Arrange
        existingTask();
        when(assignments.findById(taskId)).thenReturn(Optional.of(new TaskAssignmentDataModel(taskId, userId)));

        // Act / Assert
        assertThatThrownBy(() -> service.remove(taskId, UUID.randomUUID()))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        error -> assertThat(error.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        verify(assignments, never()).delete(any());
    }
}
