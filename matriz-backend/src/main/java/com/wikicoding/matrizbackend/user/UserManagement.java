package com.wikicoding.matrizbackend.user;

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

    @Transactional(readOnly = true)
    public ManagedUser requireActive(UUID id) {
        UserDataModel user = users.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return new ManagedUser(user.getId(), user.getKeycloakSubject(), user.getEmail(), user.getTeamId());
    }

    @Transactional(readOnly = true)
    public ManagedUser requireActiveSubject(String subject) {
        UserDataModel user = users.findByKeycloakSubjectAndDeletedFalse(subject)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Active profile required"));
        return new ManagedUser(user.getId(), user.getKeycloakSubject(), user.getEmail(), user.getTeamId());
    }

    public record ManagedUser(UUID id, String subject, String email, UUID teamId) {}
}
