package com.wikicoding.matrizbackend.team.controllers;

import com.wikicoding.matrizbackend.team.dtos.CreateTeamRequest;
import com.wikicoding.matrizbackend.team.dtos.TeamResponse;
import com.wikicoding.matrizbackend.team.services.TeamService;
import jakarta.validation.Valid;


import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** HTTP adapter for Administrator team creation. */
@RestController
@RequestMapping("/api/v1/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;

    @PostMapping
    public ResponseEntity<TeamResponse> create(@Valid @RequestBody CreateTeamRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(TeamResponse.from(teamService.create(request.name())));
    }

}
