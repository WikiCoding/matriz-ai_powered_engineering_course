package com.wikicoding.matrizbackend.user.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;
import java.util.Optional;

public interface UserRepository extends JpaRepository<UserDataModel, UUID> {

    boolean existsByTeamId(UUID teamId);

    Optional<UserDataModel> findByIdAndDeletedFalse(UUID id);
    Optional<UserDataModel> findByKeycloakSubjectAndDeletedFalse(String subject);
}
