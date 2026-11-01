package com.wikicoding.matrizbackend.team.controllers;

import com.wikicoding.matrizbackend.team.exceptions.GlobalExceptionHandler;
import com.wikicoding.matrizbackend.team.exceptions.TeamInUseException;
import com.wikicoding.matrizbackend.team.exceptions.TeamNotFoundException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    @Test
    void mapsMissingTeamToNotFoundError() {
        // Arrange
        UUID teamId = UUID.randomUUID();
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        // Act
        var response = handler.handleTeamNotFound(new TeamNotFoundException(teamId));

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isEqualTo(new GlobalExceptionHandler.ErrorResponse(
                "TEAM_NOT_FOUND", "Team " + teamId + " was not found"));
    }

    @Test
    void mapsOccupiedTeamToConflictError() {
        // Arrange
        UUID teamId = UUID.randomUUID();
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        // Act
        var response = handler.handleTeamInUse(new TeamInUseException(teamId));

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isEqualTo(new GlobalExceptionHandler.ErrorResponse(
                "TEAM_IN_USE", "Team " + teamId + " still has users or projects"));
    }
}
