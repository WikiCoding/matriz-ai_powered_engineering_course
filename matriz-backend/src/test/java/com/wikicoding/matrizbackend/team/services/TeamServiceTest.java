package com.wikicoding.matrizbackend.team.services;

import com.wikicoding.matrizbackend.team.persistence.TeamDataModel;
import com.wikicoding.matrizbackend.team.persistence.TeamRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class TeamServiceTest {

    @Test
    void createsTeamWithGeneratedIdAndProvidedName() {
        // Arrange
        TeamRepository repository = mock(TeamRepository.class);
        TeamService service = new TeamService(repository);
        ArgumentCaptor<TeamDataModel> savedTeam = ArgumentCaptor.forClass(TeamDataModel.class);

        // Act
        var team = service.create("Platform");

        // Assert
        verify(repository).save(savedTeam.capture());
        assertThat(team.getId()).isEqualTo(savedTeam.getValue().getId());
        assertThat(team.getName()).isEqualTo("Platform");
    }

}
