package com.wikicoding.matrizbackend.projectmanagement;

import java.util.UUID;

public interface ProjectTeamLookup {
    boolean exists(UUID teamId);
}
