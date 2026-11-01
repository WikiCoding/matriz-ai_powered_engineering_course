package com.wikicoding.matrizbackend.projectmanagement.dtos.tasks;

import com.wikicoding.matrizbackend.projectmanagement.domain.task.Priority;
import com.wikicoding.matrizbackend.projectmanagement.domain.task.Status;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record TaskUpdateRequest(@Size(min = 3, max = 150) String title,
                                @Size(max = 2000) String description,
                                Short effort, Status status, Priority priority, UUID projectId) {}
