package com.wikicoding.matrizbackend.user.services;

import com.wikicoding.matrizbackend.user.Profile;
import com.wikicoding.matrizbackend.user.UserAccounts;
import com.wikicoding.matrizbackend.user.persistence.UserDataModel;
import com.wikicoding.matrizbackend.user.persistence.UserRepository;
import java.util.UUID;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import com.wikicoding.matrizbackend.user.domain.ProvisioningStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class UserService implements UserAccounts {
    private final UserRepository userRepository;

    @Override
    @Transactional
    public Profile createUser(String subject, String email, UUID teamId) {
        UUID id = UUID.randomUUID();
        userRepository.saveAndFlush(new UserDataModel(id, subject, email, teamId,
                ProvisioningStatus.COMPLETED, UUID.randomUUID()));
        return new Profile(id, email);
    }

    @Transactional(readOnly = true)
    public Page<ManagedUser> findAllUsersPagedAndSorted(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid pagination parameters");
        }
        return userRepository.findByDeletedFalseAndProvisioningStatus(ProvisioningStatus.COMPLETED,
                PageRequest.of(page, size, Sort.by(Sort.Order.asc("email"), Sort.Order.asc("id"))))
                .map(user -> new ManagedUser(user.getId(), user.getKeycloakSubject(), user.getEmail(), user.getTeamId()));
    }

    @Transactional(readOnly = true)
    public ManagedUser requireActive(UUID id) {
        UserDataModel user = userRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return new ManagedUser(user.getId(), user.getKeycloakSubject(), user.getEmail(), user.getTeamId());
    }

    @Override
    @Transactional(readOnly = true)
    public UUID requireActiveTeamId(UUID userId) {
        return requireActive(userId).teamId();
    }

    @Transactional(readOnly = true)
    public ManagedUser requireActiveSubject(String subject) {
        UserDataModel user = userRepository.findByKeycloakSubjectAndDeletedFalse(subject)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Active profile required"));
        return new ManagedUser(user.getId(), user.getKeycloakSubject(), user.getEmail(), user.getTeamId());
    }

    @Transactional(readOnly = true)
    public List<ManagedUser> findActiveTeamMembers(UUID teamId) {
        return userRepository.findByTeamIdAndDeletedFalseAndProvisioningStatus(teamId, ProvisioningStatus.COMPLETED)
                .stream().map(user -> new ManagedUser(user.getId(), user.getKeycloakSubject(),
                        user.getEmail(), user.getTeamId())).toList();
    }

    public record ManagedUser(UUID id, String subject, String email, UUID teamId) {}
}
