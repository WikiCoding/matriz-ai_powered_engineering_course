package com.wikicoding.matrizbackend.projectmanagement;

import java.util.UUID;

public interface UserEligibility {
    void requireSoftwareEngineerInTeam(UUID userId, UUID teamId);
}
