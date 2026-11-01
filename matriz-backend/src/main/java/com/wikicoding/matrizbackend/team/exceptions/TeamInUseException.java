package com.wikicoding.matrizbackend.team.exceptions;

import java.util.UUID;

public class TeamInUseException extends RuntimeException {

    public TeamInUseException(UUID teamId) {
        super("Team " + teamId + " still has users or projects");
    }

    public TeamInUseException(UUID teamId, Throwable cause) {
        super("Team " + teamId + " still has users or projects", cause);
    }
}
