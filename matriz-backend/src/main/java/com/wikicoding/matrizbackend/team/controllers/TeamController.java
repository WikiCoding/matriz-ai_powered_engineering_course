package com.wikicoding.matrizbackend.team.controllers;

import com.wikicoding.matrizbackend.team.dtos.CreateTeamRequest;
import com.wikicoding.matrizbackend.team.dtos.TeamResponse;
import com.wikicoding.matrizbackend.team.services.TeamService;
import jakarta.validation.Valid;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** HTTP adapter for Administrator team lifecycle operations. */
@RestController
@RequestMapping("/api/v1/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;

    @PostMapping
    public ResponseEntity<TeamResponse> create(@Valid @RequestBody CreateTeamRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(TeamResponse.from(teamService.create(request.name())));
    }

    @PatchMapping("/{teamId}")
    public TeamResponse update(@PathVariable UUID teamId,
                               @Valid @RequestBody CreateTeamRequest request) {
        return TeamResponse.from(teamService.update(teamId, request.name()));
    }

    @DeleteMapping("/{teamId}")
    public ResponseEntity<Void> delete(@PathVariable UUID teamId) {
        teamService.delete(teamId);
        return ResponseEntity.noContent().build();
    }
}
