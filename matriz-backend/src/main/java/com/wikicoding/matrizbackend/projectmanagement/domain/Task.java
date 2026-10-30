package com.wikicoding.matrizbackend.projectmanagement.domain;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** A task belonging to a project. */
@Getter
@EqualsAndHashCode
@ToString
public class Task {

    private static final Set<Short> ALLOWED_EFFORTS = Set.of((short) 1, (short) 2, (short) 3, (short) 5, (short) 8, (short) 13);

    private final UUID id;
    private final UUID projectId;
    private final String title;
    private final String description;
    private final short effort;
    private final Status status;
    private final Priority priority;
    private final Instant createdAt;
    private final Instant updatedAt;

    public Task(UUID id, UUID projectId, String title, String description, short effort,
                Status status, Priority priority, Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id is required");
        this.projectId = Objects.requireNonNull(projectId, "projectId is required");
        this.title = requiredNonBlankInRange(title, "title", 3, 150);
        this.description = requiredAtMost(description, "description", 2000);
        if (!ALLOWED_EFFORTS.contains(effort)) {
            throw new IllegalArgumentException("effort must be one of 1, 2, 3, 5, 8, 13");
        }
        this.effort = effort;
        this.status = Objects.requireNonNull(status, "status is required");
        this.priority = Objects.requireNonNull(priority, "priority is required");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt is required");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt is required");
    }

    private static String requiredNonBlankInRange(String value, String field, int minimum, int maximum) {
        if (value == null || value.isBlank() || value.length() < minimum || value.length() > maximum) {
            throw new IllegalArgumentException(field + " must be non-blank and between " + minimum + " and " + maximum + " characters");
        }
        return value;
    }

    private static String requiredAtMost(String value, String field, int maximum) {
        if (value == null || value.length() > maximum) {
            throw new IllegalArgumentException(field + " is required and must not exceed " + maximum + " characters");
        }
        return value;
    }

    public enum Status {
        TODO,
        IN_PROGRESS,
        DONE
    }

    public enum Priority {
        LOW,
        MEDIUM,
        HIGH
    }
}
