package com.wikicoding.matrizbackend.team.services;

import com.wikicoding.matrizbackend.team.TeamReferenceChecker;
import com.wikicoding.matrizbackend.team.persistence.TeamDataModel;
import com.wikicoding.matrizbackend.team.persistence.TeamRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TeamServiceTest {

    @Test
    void createsTeamWithGeneratedIdAndProvidedName() {
        // Arrange
        TeamRepository repository = mock(TeamRepository.class);
        TeamService service = new TeamService(repository, List.of());
        ArgumentCaptor<TeamDataModel> savedTeam = ArgumentCaptor.forClass(TeamDataModel.class);

        // Act
        var team = service.create("Platform");

        // Assert
        verify(repository).save(savedTeam.capture());
        assertThat(team.getId()).isEqualTo(savedTeam.getValue().getId());
        assertThat(team.getName()).isEqualTo("Platform");
    }

    @Test
    void updatesExistingTeam() {
        // Arrange
        UUID teamId = UUID.randomUUID();
        TeamRepository repository = mock(TeamRepository.class);
        when(repository.existsById(teamId)).thenReturn(true);
        TeamService service = new TeamService(repository, List.of());
        ArgumentCaptor<TeamDataModel> savedTeam = ArgumentCaptor.forClass(TeamDataModel.class);

        // Act
        var team = service.update(teamId, "Delivery");

        // Assert
        verify(repository).save(savedTeam.capture());
        assertThat(team.getId()).isEqualTo(teamId);
        assertThat(savedTeam.getValue().getName()).isEqualTo("Delivery");
    }

    @Test
    void rejectsUpdateForUnknownTeamWithoutSaving() {
        // Arrange
        UUID teamId = UUID.randomUUID();
        TeamRepository repository = mock(TeamRepository.class);
        when(repository.existsById(teamId)).thenReturn(false);
        TeamService service = new TeamService(repository, List.of());

        // Act
        var action = (org.assertj.core.api.ThrowableAssert.ThrowingCallable) () -> service.update(teamId, "Delivery");

        // Assert
        assertThatThrownBy(action).isInstanceOf(TeamNotFoundException.class);
        verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsDeletionWhenAnotherModuleStillReferencesTeam() {
        // Arrange
        UUID teamId = UUID.randomUUID();
        TeamRepository repository = mock(TeamRepository.class);
        TeamDataModel team = new TeamDataModel(teamId, "Platform");
        TeamReferenceChecker references = mock(TeamReferenceChecker.class);
        when(repository.findById(teamId)).thenReturn(Optional.of(team));
        when(references.hasReferenceTo(teamId)).thenReturn(true);
        TeamService service = new TeamService(repository, List.of(references));

        // Act
        var action = (org.assertj.core.api.ThrowableAssert.ThrowingCallable) () -> service.delete(teamId);

        // Assert
        assertThatThrownBy(action).isInstanceOf(TeamInUseException.class);
        verify(repository, never()).delete(team);
        verify(repository, never()).flush();
    }

    @Test
    void deletesAnUnreferencedTeam() {
        // Arrange
        UUID teamId = UUID.randomUUID();
        TeamRepository repository = mock(TeamRepository.class);
        TeamDataModel team = new TeamDataModel(teamId, "Platform");
        TeamReferenceChecker references = mock(TeamReferenceChecker.class);
        when(repository.findById(teamId)).thenReturn(Optional.of(team));
        when(references.hasReferenceTo(teamId)).thenReturn(false);
        TeamService service = new TeamService(repository, List.of(references));

        // Act
        service.delete(teamId);

        // Assert
        verify(repository).delete(team);
        verify(repository).flush();
    }
}
