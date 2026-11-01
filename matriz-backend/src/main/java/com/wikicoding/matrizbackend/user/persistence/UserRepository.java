package com.wikicoding.matrizbackend.user.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.wikicoding.matrizbackend.user.domain.User.ProvisioningStatus;

public interface UserRepository extends JpaRepository<UserDataModel, UUID> {

    Optional<UserDataModel> findByIdAndDeletedFalse(UUID id);
    Optional<UserDataModel> findByKeycloakSubjectAndDeletedFalse(String subject);
    Page<UserDataModel> findByDeletedFalseAndProvisioningStatus(ProvisioningStatus status, Pageable pageable);
}
