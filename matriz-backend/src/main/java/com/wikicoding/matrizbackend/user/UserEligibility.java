package com.wikicoding.matrizbackend.user;

import java.util.UUID;

public interface UserEligibility {
    void requireSoftwareEngineerInTeam(UUID userId, UUID teamId);
}
