package com.wikicoding.matrizbackend.user.persistence;

import com.wikicoding.matrizbackend.user.domain.User.ProvisioningStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** JPA representation of the application-owned {@code app_user} table. */
@Entity
@Table(name = "app_user")
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@AllArgsConstructor
public class UserDataModel {

    @Id
    private UUID id;

    @Column(name = "keycloak_subject")
    private String keycloakSubject;

    @Column(nullable = false)
    private String email;

    @Column(name = "team_id", nullable = false)
    private UUID teamId;

    @Column(nullable = false)
    private boolean deleted;

    @Enumerated(EnumType.STRING)
    @Column(name = "provisioning_status", nullable = false)
    private ProvisioningStatus provisioningStatus;

    @Column(name = "provisioning_correlation_id", nullable = false)
    private UUID provisioningCorrelationId;

    public UserDataModel(UUID id, String keycloakSubject, String email, UUID teamId,
            ProvisioningStatus provisioningStatus, UUID provisioningCorrelationId) {
        this.id = id;
        this.keycloakSubject = keycloakSubject;
        this.email = email;
        this.teamId = teamId;
        this.provisioningStatus = provisioningStatus;
        this.provisioningCorrelationId = provisioningCorrelationId;
    }

}
