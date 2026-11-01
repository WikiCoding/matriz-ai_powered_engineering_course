package com.wikicoding.matrizbackend.projectmanagement.dtos.tasks;

import com.wikicoding.matrizbackend.projectmanagement.domain.task.Priority;
import com.wikicoding.matrizbackend.projectmanagement.domain.task.Status;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record TaskRequest(@NotNull UUID projectId,
                          @NotBlank @Size(min = 3, max = 150) String title,
                          @NotNull @Size(max = 2000) String description,
                          @NotNull Short effort, Status status, Priority priority) {}
