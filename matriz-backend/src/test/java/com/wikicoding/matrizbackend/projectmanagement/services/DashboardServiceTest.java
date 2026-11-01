package com.wikicoding.matrizbackend.projectmanagement.services;

import com.wikicoding.matrizbackend.projectmanagement.domain.task.Status;
import com.wikicoding.matrizbackend.projectmanagement.persistence.project.ProjectDataModel;
import com.wikicoding.matrizbackend.projectmanagement.persistence.project.ProjectRepository;
import com.wikicoding.matrizbackend.projectmanagement.persistence.task.TaskRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DashboardServiceTest {
    private final ProjectRepository projectRepository = mock(ProjectRepository.class);
    private final TaskRepository taskRepository = mock(TaskRepository.class);
    private final DashboardService dashboardService = new DashboardService(projectRepository, taskRepository);

    @Test
    void countsAllStatusesAcrossProjects() {
        // Arrange
        UUID team = UUID.randomUUID(), owner = UUID.randomUUID();
        when(projectRepository.findAll(any(Sort.class))).thenReturn(List.of(
                new ProjectDataModel(UUID.randomUUID(), "Alpha", "", team, owner),
                new ProjectDataModel(UUID.randomUUID(), "Beta", "", team, owner)));
        TaskRepository.StatusCount done = mock(TaskRepository.StatusCount.class);
        when(done.getStatus()).thenReturn(Status.DONE);
        when(done.getTotal()).thenReturn(7L);
        when(taskRepository.countByStatus()).thenReturn(List.of(done));

        // Act
        var result = dashboardService.getDashboard(false);

        // Assert
        assertThat(result.projects()).extracting("name").containsExactly("Alpha", "Beta");
        assertThat(result.taskCounts()).containsEntry("TODO", 0L).containsEntry("IN_PROGRESS", 0L)
                .containsEntry("DONE", 7L);
        verify(taskRepository).countByStatus();
    }

    @Test
    void administratorHasNoProjectContent() {
        // Arrange
        // Act
        var result = dashboardService.getDashboard(true);

        // Assert
        assertThat(result.projects()).isEmpty();
        assertThat(result.taskCounts()).containsEntry("TODO", 0L).containsEntry("DONE", 0L);
        verifyNoInteractions(projectRepository, taskRepository);
    }
}
