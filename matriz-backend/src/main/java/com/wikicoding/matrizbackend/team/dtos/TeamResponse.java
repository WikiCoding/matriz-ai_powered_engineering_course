package com.wikicoding.matrizbackend.team.dtos;

import com.wikicoding.matrizbackend.team.domain.Team;

import java.util.UUID;

public record TeamResponse(UUID id, String name) {
    public static TeamResponse from(Team team) {
        return new TeamResponse(team.getId(), team.getName());
    }
}
