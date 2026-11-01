package com.wikicoding.matrizbackend.team.exceptions;

import java.util.UUID;

public class TeamNotFoundException extends RuntimeException {

    public TeamNotFoundException(UUID teamId) {
        super("Team " + teamId + " was not found");
    }
}
