package com.wikicoding.matrizbackend.projectmanagement.domain.project;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Objects;
import java.util.UUID;

/** A project and its owning team and creator references. */
@Getter
@EqualsAndHashCode
@ToString
public class Project {

    private final UUID id;
    private final String name;
    private final String description;
    private final UUID teamId;
    private final UUID ownerId;

    public Project(UUID id, String name, String description, UUID teamId, UUID ownerId) {
        this.id = Objects.requireNonNull(id, "id is required");
        this.name = requiredNonBlankInRange(name, "name", 3, 100);
        this.description = requiredAtMost(description, "description", 1000);
        this.teamId = Objects.requireNonNull(teamId, "teamId is required");
        this.ownerId = Objects.requireNonNull(ownerId, "ownerId is required");
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
}
