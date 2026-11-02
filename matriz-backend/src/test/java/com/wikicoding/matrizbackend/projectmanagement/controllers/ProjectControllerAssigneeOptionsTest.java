package com.wikicoding.matrizbackend.projectmanagement.controllers;

import com.wikicoding.matrizbackend.projectmanagement.AssigneeOptionsLookup;
import com.wikicoding.matrizbackend.projectmanagement.domain.project.Project;
import com.wikicoding.matrizbackend.projectmanagement.services.ProjectService;
import com.wikicoding.matrizbackend.projectmanagement.services.TaskService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ProjectControllerAssigneeOptionsTest {
    @Test
    void findsOptionsForTheProjectTeamThroughProjectService() {
        // Arrange
        var projects = mock(ProjectService.class);
        var options = mock(AssigneeOptionsLookup.class);
        var controller = new ProjectController(projects, mock(TaskService.class), options);
        UUID projectId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        var expected = List.of(new AssigneeOptionsLookup.Option(UUID.randomUUID(), "Ana Silva"));
        when(projects.findProjectById(projectId)).thenReturn(
                new Project(projectId, "Alpha", "", teamId, UUID.randomUUID()));
        when(options.eligibleForTeam(teamId)).thenReturn(expected);

        // Act
        var actual = controller.findAssigneeOptions(projectId);

        // Assert
        assertThat(actual).isEqualTo(expected);
        verify(projects).findProjectById(projectId);
        verify(options).eligibleForTeam(teamId);
    }

    @Test
    void missingProjectDoesNotQueryEligibleUsers() {
        // Arrange
        var projects = mock(ProjectService.class);
        var options = mock(AssigneeOptionsLookup.class);
        var controller = new ProjectController(projects, mock(TaskService.class), options);
        UUID projectId = UUID.randomUUID();
        when(projects.findProjectById(projectId))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));

        // Act / Assert
        assertThatThrownBy(() -> controller.findAssigneeOptions(projectId))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        error -> assertThat(error.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        verifyNoInteractions(options);
    }
}
