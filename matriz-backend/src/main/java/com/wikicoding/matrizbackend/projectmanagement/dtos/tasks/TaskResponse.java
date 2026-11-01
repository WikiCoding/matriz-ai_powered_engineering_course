package com.wikicoding.matrizbackend.projectmanagement.dtos.tasks;

import com.wikicoding.matrizbackend.projectmanagement.domain.task.Status;
import com.wikicoding.matrizbackend.projectmanagement.domain.task.Priority;

import com.wikicoding.matrizbackend.projectmanagement.domain.task.Task;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public record TaskResponse(UUID id, UUID projectId, String title, String description, short effort,
                           Status status, Priority priority, String createdAt, String updatedAt) {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    public static TaskResponse from(Task task) {
        return new TaskResponse(task.getId(), task.getProjectId(), task.getTitle(), task.getDescription(),
                task.getEffort(), task.getStatus(), task.getPriority(), DATE_FORMAT.format(task.getCreatedAt()),
                DATE_FORMAT.format(task.getUpdatedAt()));
    }
}
