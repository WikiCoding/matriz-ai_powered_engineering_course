package com.wikicoding.matrizbackend.projectmanagement;

import java.util.List;
import java.util.UUID;

public interface AssigneeOptionsLookup {
    List<Option> eligibleForTeam(UUID teamId);
    record Option(UUID id, String displayName) {}
}
