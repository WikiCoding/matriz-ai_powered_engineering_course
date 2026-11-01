package com.wikicoding.matrizbackend.team.dtos;

import jakarta.validation.constraints.NotBlank;

public record CreateTeamRequest(@NotBlank String name) {
}
