package com.wikicoding.matrizbackend.projectmanagement;

import java.util.UUID;

/** Assignment removal requested by the User module in the caller's transaction. */
public interface UserAssignmentCleanup {
    void removeForUser(UUID userId);
}
