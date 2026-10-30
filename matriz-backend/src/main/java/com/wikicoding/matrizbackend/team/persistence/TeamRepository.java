package com.wikicoding.matrizbackend.team.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TeamRepository extends JpaRepository<TeamDataModel, UUID> {
}
