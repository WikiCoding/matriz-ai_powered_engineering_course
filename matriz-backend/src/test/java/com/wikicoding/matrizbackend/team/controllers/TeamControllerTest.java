package com.wikicoding.matrizbackend.team.controllers;

import com.wikicoding.matrizbackend.team.dtos.CreateTeamRequest;
import com.wikicoding.matrizbackend.team.dtos.TeamResponse;
import com.wikicoding.matrizbackend.team.domain.Team;
import com.wikicoding.matrizbackend.team.services.TeamService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TeamControllerTest {

    @Test
    void administratorCanCreateTeam() {
        // Arrange
        TeamService service = mock(TeamService.class);
        TeamController controller = new TeamController(service);
        Team team = new Team(UUID.randomUUID(), "Platform");
        when(service.create("Platform")).thenReturn(team);

        // Act
        var response = controller.create(new CreateTeamRequest("Platform"));

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(new TeamResponse(team.getId(), "Platform"));
        verify(service).create("Platform");
    }

    @Test
    void deletesAnEmptyTeam() {
        // Arrange
        UUID teamId = UUID.randomUUID();
        TeamService service = mock(TeamService.class);
        TeamController controller = new TeamController(service);

        // Act
        var response = controller.delete(teamId);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(service).delete(teamId);
    }

}
