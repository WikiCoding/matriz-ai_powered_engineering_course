package com.wikicoding.matrizbackend.user.domain;

import org.junit.jupiter.api.Test;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class UserTest {

    @Test
    void createsPendingUserWithoutKeycloakSubject() {
        // Arrange
        UUID id = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        UUID correlationId = UUID.randomUUID();

        // Act
        User user = new User(id, null, "engineer@example.com", teamId,
                User.ProvisioningStatus.PENDING, correlationId);

        // Assert
        assertThat(user.getId()).isEqualTo(id);
        assertThat(user.getKeycloakSubject()).isNull();
        assertThat(user.getEmail()).isEqualTo("engineer@example.com");
        assertThat(user.getTeamId()).isEqualTo(teamId);
        assertThat(user.getProvisioningStatus()).isEqualTo(User.ProvisioningStatus.PENDING);
        assertThat(user.getProvisioningCorrelationId()).isEqualTo(correlationId);
    }

    @Test
    void rejectsCompletedUserWithoutKeycloakSubject() {
        // Arrange
        UUID id = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        UUID correlationId = UUID.randomUUID();

        // Act
        ThrowingCallable action = () -> new User(id, null, "engineer@example.com", teamId,
                User.ProvisioningStatus.COMPLETED, correlationId);

        // Assert
        assertThatIllegalArgumentException().isThrownBy(action)
                .withMessage("a completed user requires a Keycloak subject");
    }

    @Test
    void rejectsMissingRequiredReferences() {
        // Arrange
        UUID id = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        UUID correlationId = UUID.randomUUID();

        // Act
        ThrowingCallable missingId = () -> new User(null, "subject", "engineer@example.com", teamId,
                User.ProvisioningStatus.COMPLETED, correlationId);
        ThrowingCallable missingEmail = () -> new User(id, "subject", null, teamId,
                User.ProvisioningStatus.COMPLETED, correlationId);
        ThrowingCallable missingTeam = () -> new User(id, "subject", "engineer@example.com", null,
                User.ProvisioningStatus.COMPLETED, correlationId);
        ThrowingCallable missingStatus = () -> new User(id, "subject", "engineer@example.com", teamId,
                null, correlationId);
        ThrowingCallable missingCorrelation = () -> new User(id, "subject", "engineer@example.com", teamId,
                User.ProvisioningStatus.COMPLETED, null);

        // Assert
        assertThatNullPointerException().isThrownBy(missingId).withMessage("id is required");
        assertThatNullPointerException().isThrownBy(missingEmail).withMessage("email is required");
        assertThatNullPointerException().isThrownBy(missingTeam).withMessage("teamId is required");
        assertThatNullPointerException().isThrownBy(missingStatus).withMessage("provisioningStatus is required");
        assertThatNullPointerException().isThrownBy(missingCorrelation)
                .withMessage("provisioningCorrelationId is required");
    }
}
