package com.wikicoding.matrizbackend.team.controllers;

import com.wikicoding.matrizbackend.team.dtos.CreateTeamRequest;
import com.wikicoding.matrizbackend.team.dtos.TeamResponse;
import com.wikicoding.matrizbackend.team.services.TeamService;
import jakarta.validation.Valid;


import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** HTTP adapter for Administrator team creation. */
@RestController
@RequestMapping("/api/v1/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;

    @GetMapping
    public TeamPage findAllTeams(@RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "20") int size) {
        var result = teamService.findAllTeamsPagedAndSorted(page, size);
        return new TeamPage(result.getContent().stream().map(TeamResponse::from).toList(),
                result.getTotalElements(), result.getTotalPages(), result.getNumber(), result.getSize());
    }

    public record TeamPage(List<TeamResponse> content, long totalElements, int totalPages, int number, int size) {}

    @PostMapping
    public ResponseEntity<TeamResponse> createTeam(@Valid @RequestBody CreateTeamRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(TeamResponse.from(teamService.createTeam(request.name())));
    }

}
