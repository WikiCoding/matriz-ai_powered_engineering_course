package com.wikicoding.matrizbackend.user.domain;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Objects;
import java.util.UUID;

/** Application profile linked to a Keycloak identity. */
@Getter
@EqualsAndHashCode
@ToString
public class User {

    private final UUID id;
    private final String keycloakSubject;
    private final String email;
    private final UUID teamId;
    private final ProvisioningStatus provisioningStatus;
    private final UUID provisioningCorrelationId;

    public User(UUID id, String keycloakSubject, String email, UUID teamId,
                ProvisioningStatus provisioningStatus, UUID provisioningCorrelationId) {
        this.id = Objects.requireNonNull(id, "id is required");
        this.email = Objects.requireNonNull(email, "email is required");
        this.teamId = Objects.requireNonNull(teamId, "teamId is required");
        this.provisioningStatus = Objects.requireNonNull(provisioningStatus, "provisioningStatus is required");
        this.provisioningCorrelationId = Objects.requireNonNull(
                provisioningCorrelationId, "provisioningCorrelationId is required");
        if (provisioningStatus == ProvisioningStatus.COMPLETED && keycloakSubject == null) {
            throw new IllegalArgumentException("a completed user requires a Keycloak subject");
        }
        this.keycloakSubject = keycloakSubject;
    }

    public enum ProvisioningStatus {
        PENDING,
        COMPLETED
    }
}
