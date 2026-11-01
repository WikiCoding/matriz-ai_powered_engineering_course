package com.wikicoding.matrizbackend.projectmanagement.persistence.project;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** JPA representation of the {@code project} table. */
@Entity
@Table(name = "project")
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@AllArgsConstructor
public class ProjectDataModel {

    @Id
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 1000)
    private String description;

    @Column(name = "team_id", nullable = false)
    private UUID teamId;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    public void changeDetails(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public void assignTeamToProject(UUID teamId) {
        this.teamId = teamId;
    }

}
