package com.wikicoding.matrizbackend.team;

import java.util.UUID;

public interface TeamLookup {
    boolean exists(UUID teamId);
}
