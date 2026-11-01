package com.wikicoding.matrizbackend.projectmanagement.services;

import com.wikicoding.matrizbackend.projectmanagement.persistence.project.ProjectDataModel;
import com.wikicoding.matrizbackend.projectmanagement.ProjectOwnerLookup;
import com.wikicoding.matrizbackend.projectmanagement.persistence.project.ProjectRepository;
import com.wikicoding.matrizbackend.projectmanagement.persistence.taskassignment.TaskAssignmentRepository;
import com.wikicoding.matrizbackend.projectmanagement.ProjectTeamLookup;
import java.util.Optional;
import java.util.UUID;
import java.util.List;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;

class ProjectServiceTest {
    @Test
    void listsProjectsWithDefaultPageSizeAndStableAlphabeticSort() {
        // Arrange
        var projects = mock(ProjectRepository.class);
        var service = new ProjectService(projects, mock(TaskAssignmentRepository.class),
                mock(ProjectTeamLookup.class), mock(ProjectOwnerLookup.class));
        var stored = new ProjectDataModel(UUID.randomUUID(), "Alpha", "", UUID.randomUUID(), UUID.randomUUID());
        when(projects.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(stored)));

        // Act
        var result = service.findAllProjects(0, 20);

        // Assert
        assertThat(result.getContent()).extracting("name").containsExactly("Alpha");
        var requested = ArgumentCaptor.forClass(Pageable.class);
        verify(projects).findAll(requested.capture());
        assertThat(requested.getValue().getPageSize()).isEqualTo(20);
        assertThat(requested.getValue().getSort().getOrderFor("name").getDirection()).isEqualTo(org.springframework.data.domain.Sort.Direction.ASC);
        assertThat(requested.getValue().getSort().getOrderFor("id").getDirection()).isEqualTo(org.springframework.data.domain.Sort.Direction.ASC);
    }

    @Test
    void findsProjectByIdOrReturnsNotFound() {
        // Arrange
        var projects = mock(ProjectRepository.class);
        var service = new ProjectService(projects, mock(TaskAssignmentRepository.class),
                mock(ProjectTeamLookup.class), mock(ProjectOwnerLookup.class));
        UUID id = UUID.randomUUID();
        when(projects.findById(id)).thenReturn(Optional.of(new ProjectDataModel(id, "Alpha", "", UUID.randomUUID(), UUID.randomUUID())));

        // Act
        var found = service.findProjectById(id);

        // Assert
        assertThat(found.getId()).isEqualTo(id);
        assertThatThrownBy(() -> service.findProjectById(UUID.randomUUID()))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        error -> assertThat(error.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void rejectsInvalidPaginationWithoutQueryingRepository() {
        // Arrange
        var projects = mock(ProjectRepository.class);
        var service = new ProjectService(projects, mock(TaskAssignmentRepository.class),
                mock(ProjectTeamLookup.class), mock(ProjectOwnerLookup.class));

        // Act / Assert
        assertThatThrownBy(() -> service.findAllProjects(-1, 20))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        error -> assertThat(error.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        verifyNoInteractions(projects);
    }
    @Test
    void createUsesAuthenticatedOwner() {
        // Arrange
        var projects = mock(ProjectRepository.class);
        var assignments = mock(TaskAssignmentRepository.class);
        var teams = mock(ProjectTeamLookup.class);
        var owners = mock(ProjectOwnerLookup.class);
        var service = new ProjectService(projects, assignments, teams, owners);
        UUID teamId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        when(teams.exists(teamId)).thenReturn(true);
        when(owners.requireActiveOwnerId("subject")).thenReturn(ownerId);

        // Act
        var created = service.createProject("Platform", "", teamId, "subject");

        // Assert
        assertThat(created.getOwnerId()).isEqualTo(ownerId);
        assertThat(created.getTeamId()).isEqualTo(teamId);
        verify(projects).save(any(ProjectDataModel.class));
    }

    @Test
    void teamChangeRemovesInvalidAssignmentsAndKeepsOwner() {
        // Arrange
        var projects = mock(ProjectRepository.class);
        var assignments = mock(TaskAssignmentRepository.class);
        var teams = mock(ProjectTeamLookup.class);
        var service = new ProjectService(projects, assignments, teams, mock(ProjectOwnerLookup.class));
        UUID projectId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID newTeamId = UUID.randomUUID();
        var stored = new ProjectDataModel(projectId, "Old", "", UUID.randomUUID(), ownerId);
        when(projects.findById(projectId)).thenReturn(Optional.of(stored));
        when(teams.exists(newTeamId)).thenReturn(true);

        // Act
        var updated = service.updateProject(projectId, "New", "Updated", newTeamId);

        // Assert
        assertThat(updated.getOwnerId()).isEqualTo(ownerId);
        assertThat(stored.getTeamId()).isEqualTo(newTeamId);
        verify(assignments).deleteInvalidForProject(projectId, newTeamId);
        verify(projects).flush();
    }

    @Test
    void invalidUpdateDoesNotChangeStoredProject() {
        // Arrange
        var projects = mock(ProjectRepository.class);
        var assignments = mock(TaskAssignmentRepository.class);
        var teams = mock(ProjectTeamLookup.class);
        var service = new ProjectService(projects, assignments, teams, mock(ProjectOwnerLookup.class));
        UUID projectId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        var stored = new ProjectDataModel(projectId, "Original", "", teamId, UUID.randomUUID());
        when(projects.findById(projectId)).thenReturn(Optional.of(stored));
        when(teams.exists(teamId)).thenReturn(true);

        // Act / Assert
        assertThatThrownBy(() -> service.updateProject(projectId, "  ", "", teamId))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(stored.getName()).isEqualTo("Original");
        verify(assignments, never()).deleteInvalidForProject(any(), any());
        verify(projects, never()).flush();
    }

    @Test
    void missingTeamRejectsCreationWithoutWrite() {
        // Arrange
        var projects = mock(ProjectRepository.class);
        var service = new ProjectService(projects, mock(TaskAssignmentRepository.class),
                mock(ProjectTeamLookup.class), mock(ProjectOwnerLookup.class));

        // Act / Assert
        assertThatThrownBy(() -> service.createProject("Platform", "", UUID.randomUUID(), "subject"))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        error -> assertThat(error.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        verify(projects, never()).save(any());
    }

    @Test
    void createPersistsReturnedProjectDetails() {
        // Arrange
        var projects = mock(ProjectRepository.class);
        var teams = mock(ProjectTeamLookup.class);
        var owners = mock(ProjectOwnerLookup.class);
        var service = new ProjectService(projects, mock(TaskAssignmentRepository.class), teams, owners);
        UUID teamId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        when(teams.exists(teamId)).thenReturn(true);
        when(owners.requireActiveOwnerId("subject")).thenReturn(ownerId);

        // Act
        var created = service.createProject("Platform", "Description", teamId, "subject");

        // Assert
        var saved = ArgumentCaptor.forClass(ProjectDataModel.class);
        verify(projects).save(saved.capture());
        assertThat(saved.getValue().getId()).isEqualTo(created.getId());
        assertThat(saved.getValue().getName()).isEqualTo("Platform");
        assertThat(saved.getValue().getDescription()).isEqualTo("Description");
        assertThat(saved.getValue().getTeamId()).isEqualTo(teamId);
        assertThat(saved.getValue().getOwnerId()).isEqualTo(ownerId);
    }

    @Test
    void unchangedTeamUpdatesDetailsWithoutRemovingAssignments() {
        // Arrange
        var projects = mock(ProjectRepository.class);
        var assignments = mock(TaskAssignmentRepository.class);
        var teams = mock(ProjectTeamLookup.class);
        var service = new ProjectService(projects, assignments, teams, mock(ProjectOwnerLookup.class));
        UUID projectId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        var stored = new ProjectDataModel(projectId, "Old", "Old description", teamId, ownerId);
        when(projects.findById(projectId)).thenReturn(Optional.of(stored));
        when(teams.exists(teamId)).thenReturn(true);

        // Act
        var updated = service.updateProject(projectId, "New", "New description", teamId);

        // Assert
        assertThat(updated.getId()).isEqualTo(projectId);
        assertThat(updated.getName()).isEqualTo("New");
        assertThat(stored.getName()).isEqualTo("New");
        assertThat(stored.getDescription()).isEqualTo("New description");
        assertThat(stored.getOwnerId()).isEqualTo(ownerId);
        verifyNoInteractions(assignments);
        verify(projects).flush();
    }

    @Test
    void missingProjectRejectsUpdateWithoutWrites() {
        // Arrange
        var projects = mock(ProjectRepository.class);
        var assignments = mock(TaskAssignmentRepository.class);
        var teams = mock(ProjectTeamLookup.class);
        var service = new ProjectService(projects, assignments, teams, mock(ProjectOwnerLookup.class));
        UUID projectId = UUID.randomUUID();

        // Act / Assert
        assertThatThrownBy(() -> service.updateProject(projectId, "New", "", UUID.randomUUID()))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        error -> assertThat(error.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        verifyNoInteractions(assignments, teams);
        verify(projects, never()).flush();
    }

    @Test
    void missingTeamRejectsUpdateWithoutChangingStoredProject() {
        // Arrange
        var projects = mock(ProjectRepository.class);
        var assignments = mock(TaskAssignmentRepository.class);
        var teams = mock(ProjectTeamLookup.class);
        var service = new ProjectService(projects, assignments, teams, mock(ProjectOwnerLookup.class));
        UUID projectId = UUID.randomUUID();
        UUID originalTeamId = UUID.randomUUID();
        UUID missingTeamId = UUID.randomUUID();
        var stored = new ProjectDataModel(projectId, "Original", "Original description", originalTeamId, UUID.randomUUID());
        when(projects.findById(projectId)).thenReturn(Optional.of(stored));

        // Act / Assert
        assertThatThrownBy(() -> service.updateProject(projectId, "Changed", "Changed description", missingTeamId))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        error -> assertThat(error.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        assertThat(stored.getName()).isEqualTo("Original");
        assertThat(stored.getDescription()).isEqualTo("Original description");
        assertThat(stored.getTeamId()).isEqualTo(originalTeamId);
        verifyNoInteractions(assignments);
        verify(projects, never()).flush();
    }
}
