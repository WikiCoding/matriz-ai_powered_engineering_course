package com.wikicoding.matrizbackend.projectmanagement.dtos.projects;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record ProjectRequest(@NotBlank @Size(min = 3, max = 100) String name,
                             @NotNull @Size(max = 1000) String description, @NotNull UUID teamId) {}
