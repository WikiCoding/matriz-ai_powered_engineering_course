package com.wikicoding.matrizbackend.user;

import java.util.UUID;

/** Operations exposed by the User module to the HTTP edge. */
public interface UserAccounts {
    Profile createUser(String subject, String email, UUID teamId);

    UUID requireActiveTeamId(UUID userId);
}
