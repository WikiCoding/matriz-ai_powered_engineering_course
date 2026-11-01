package com.wikicoding.matrizbackend.team.domain;

import org.junit.jupiter.api.Test;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class TeamTest {

    @Test
    void createsTeamWithItsIdentityAndName() {
        // Arrange
        UUID id = UUID.randomUUID();

        // Act
        Team team = new Team(id, "Platform");

        // Assert
        assertThat(team.getId()).isEqualTo(id);
        assertThat(team.getName()).isEqualTo("Platform");
    }

    @Test
    void rejectsMissingRequiredFields() {
        // Arrange
        UUID id = UUID.randomUUID();

        // Act
        ThrowingCallable missingId = () -> new Team(null, "Platform");
        ThrowingCallable missingName = () -> new Team(id, null);

        // Assert
        assertThatNullPointerException().isThrownBy(missingId).withMessage("id is required");
        assertThatNullPointerException().isThrownBy(missingName).withMessage("name is required");
    }

    @Test
    void rejectsBlankName() {
        // Arrange
        UUID id = UUID.randomUUID();

        // Act
        ThrowingCallable action = () -> new Team(id, "   ");

        // Assert
        assertThatIllegalArgumentException().isThrownBy(action).withMessage("name must be non-blank");
    }
}
