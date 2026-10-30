package com.wikicoding.matrizbackend.team.domain;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Objects;
import java.util.UUID;

/** A team that owns its membership and groups projects. */
@Getter
@EqualsAndHashCode
@ToString
public class Team {

    private final UUID id;
    private final String name;

    public Team(UUID id, String name) {
        this.id = Objects.requireNonNull(id, "id is required");
        this.name = Objects.requireNonNull(name, "name is required");
    }
}
