package com.wikicoding.matrizbackend.user.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserRepository extends JpaRepository<UserDataModel, UUID> {

    boolean existsByTeamId(UUID teamId);
}
