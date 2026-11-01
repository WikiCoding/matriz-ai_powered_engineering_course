package com.wikicoding.matrizbackend.projectmanagement.domain.taskassignment;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Objects;
import java.util.UUID;

/** The optional, single assignee for a task. */
@Getter
@EqualsAndHashCode
@ToString
public class TaskAssignment {

    private final UUID taskId;
    private final UUID userId;

    public TaskAssignment(UUID taskId, UUID userId) {
        this.taskId = Objects.requireNonNull(taskId, "taskId is required");
        this.userId = Objects.requireNonNull(userId, "userId is required");
    }
}
