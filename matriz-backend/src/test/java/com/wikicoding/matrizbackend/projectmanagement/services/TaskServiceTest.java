package com.wikicoding.matrizbackend.projectmanagement.services;

import com.wikicoding.matrizbackend.projectmanagement.domain.task.Status;
import com.wikicoding.matrizbackend.projectmanagement.domain.task.Priority;

import com.wikicoding.matrizbackend.projectmanagement.dtos.tasks.TaskRequest;
import com.wikicoding.matrizbackend.projectmanagement.dtos.tasks.TaskUpdateRequest;
import com.wikicoding.matrizbackend.projectmanagement.persistence.project.ProjectRepository;
import com.wikicoding.matrizbackend.projectmanagement.persistence.project.ProjectDataModel;
import com.wikicoding.matrizbackend.projectmanagement.persistence.taskassignment.TaskAssignmentRepository;
import com.wikicoding.matrizbackend.projectmanagement.persistence.task.TaskDataModel;
import com.wikicoding.matrizbackend.projectmanagement.persistence.task.TaskRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TaskServiceTest {
    private final TaskRepository tasks = mock(TaskRepository.class);
    private final ProjectRepository projects = mock(ProjectRepository.class);
    private final TaskAssignmentRepository assignments = mock(TaskAssignmentRepository.class);
    private final TaskService service = new TaskService(tasks, projects, assignments);
    private final UUID projectId = UUID.randomUUID();

    @Test
    void listsTasksWithBoundedPagingAndStableCreationSort() {
        // Arrange
        when(projects.existsById(projectId)).thenReturn(true);
        when(tasks.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(java.util.List.of()));

        // Act
        var result = service.findAllTasks(projectId, Status.TODO, Priority.HIGH, 1, 10);

        // Assert
        assertThat(result.getContent()).isEmpty();
        var page = ArgumentCaptor.forClass(Pageable.class);
        verify(tasks).findAll(any(Specification.class), page.capture());
        assertThat(page.getValue().getPageNumber()).isEqualTo(1);
        assertThat(page.getValue().getPageSize()).isEqualTo(10);
        assertThat(page.getValue().getSort().getOrderFor("createdAt").getDirection())
                .isEqualTo(org.springframework.data.domain.Sort.Direction.DESC);
        assertThat(page.getValue().getSort().getOrderFor("id")).isNotNull();
    }

    @Test
    void createDefaultsStatusAndPriorityAndRejectsInvalidEffort() {
        // Arrange
        when(projects.existsById(projectId)).thenReturn(true);

        // Act
        var created = service.createTask(new TaskRequest(projectId, "Build", "", (short) 3, null, null));

        // Assert
        assertThat(created.getStatus()).isEqualTo(Status.TODO);
        assertThat(created.getPriority()).isEqualTo(Priority.MEDIUM);
        verify(tasks).save(any(TaskDataModel.class));
        assertThatThrownBy(() -> service.createTask(new TaskRequest(projectId, "Build", "", (short) 4, null, null)))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        error -> assertThat(error.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        verify(tasks, times(1)).save(any(TaskDataModel.class));
    }

    @Test
    void movingTaskRemovesAssignmentAndUpdatesAllDetails() {
        // Arrange
        UUID taskId = UUID.randomUUID();
        UUID destination = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
        var stored = new TaskDataModel(taskId, projectId, "Old", "", (short) 1,
                Status.TODO, Priority.LOW, createdAt, createdAt, 0);
        when(tasks.findById(taskId)).thenReturn(Optional.of(stored));
        UUID destinationTeam = UUID.randomUUID();
        when(projects.findById(destination)).thenReturn(Optional.of(new ProjectDataModel(destination,
                "Destination", "", destinationTeam, UUID.randomUUID())));

        // Act
        var updated = service.updateTask(taskId, new TaskUpdateRequest("New", "Details", (short) 5,
                Status.DONE, Priority.HIGH, destination));

        // Assert
        assertThat(updated.getProjectId()).isEqualTo(destination);
        assertThat(updated.getTitle()).isEqualTo("New");
        assertThat(updated.getStatus()).isEqualTo(Status.DONE);
        assertThat(stored.getCreatedAt()).isEqualTo(createdAt);
        assertThat(stored.getUpdatedAt()).isAfter(createdAt);
        verify(assignments).deleteInvalidForTask(taskId, destinationTeam);
        verify(tasks).flush();
    }

    @Test
    void rejectedUpdateDoesNotMutateTaskOrAssignment() {
        // Arrange
        UUID taskId = UUID.randomUUID();
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        var stored = new TaskDataModel(taskId, projectId, "Old", "", (short) 1,
                Status.TODO, Priority.LOW, now, now, 0);
        when(tasks.findById(taskId)).thenReturn(Optional.of(stored));
        when(projects.findById(projectId)).thenReturn(Optional.of(new ProjectDataModel(projectId,
                "Current", "", UUID.randomUUID(), UUID.randomUUID())));

        // Act / Assert
        assertThatThrownBy(() -> service.updateTask(taskId, new TaskUpdateRequest("Bad", "", (short) 4, null, null, null)))
                .isInstanceOf(ResponseStatusException.class);
        assertThat(stored.getEffort()).isEqualTo((short) 1);
        verifyNoInteractions(assignments);
        verify(tasks, never()).flush();
    }
}
