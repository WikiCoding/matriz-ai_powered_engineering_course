package com.wikicoding.matrizbackend.user;

import com.wikicoding.matrizbackend.projectmanagement.UserAssignmentCleanup;
import com.wikicoding.matrizbackend.team.TeamLookup;
import com.wikicoding.matrizbackend.user.persistence.UserDataModel;
import com.wikicoding.matrizbackend.user.persistence.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class UserManagement {
    private final UserRepository users;
    private final UserAssignmentCleanup assignments;
    private final TeamLookup teams;

    @Transactional(readOnly = true)
    public ManagedUser requireActive(UUID id) {
        UserDataModel user = users.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return new ManagedUser(user.getId(), user.getKeycloakSubject(), user.getEmail(), user.getTeamId());
    }

    @Transactional
    public ManagedUser moveToTeam(UUID id, UUID teamId) {
        if (teamId == null || !teams.exists(teamId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Team not found");
        }
        UserDataModel user = users.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (!user.getTeamId().equals(teamId)) {
            assignments.removeForUser(id);
            user.moveToTeam(teamId);
            users.flush();
        }
        return new ManagedUser(id, user.getKeycloakSubject(), user.getEmail(), teamId);
    }

    @Transactional
    public void deleteLocal(UUID id) {
        UserDataModel user = users.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        assignments.removeForUser(id);
        user.markDeleted();
        users.flush();
    }

    @Transactional
    public void removeAssignments(UUID id) {
        requireActive(id);
        assignments.removeForUser(id);
    }

    public record ManagedUser(UUID id, String subject, String email, UUID teamId) {}
}
