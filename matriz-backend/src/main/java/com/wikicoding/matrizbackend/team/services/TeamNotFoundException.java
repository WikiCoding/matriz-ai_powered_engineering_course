package com.wikicoding.matrizbackend.team.services;

import java.util.UUID;

public class TeamNotFoundException extends RuntimeException {

    public TeamNotFoundException(UUID teamId) {
        super("Team " + teamId + " was not found");
    }
}
