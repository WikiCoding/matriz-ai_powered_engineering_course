package com.wikicoding.matrizbackend.projectmanagement.domain;

import com.wikicoding.matrizbackend.projectmanagement.domain.project.Project;
import org.junit.jupiter.api.Test;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class ProjectTest {

    @Test
    void createsProjectWithBoundaryLengthFields() {
        // Arrange
        UUID id = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        String name = "n".repeat(100);
        String description = "d".repeat(1000);

        // Act
        Project project = new Project(id, name, description, teamId, ownerId);

        // Assert
        assertThat(project.getId()).isEqualTo(id);
        assertThat(project.getName()).isEqualTo(name);
        assertThat(project.getDescription()).isEqualTo(description);
        assertThat(project.getTeamId()).isEqualTo(teamId);
        assertThat(project.getOwnerId()).isEqualTo(ownerId);
    }

    @Test
    void rejectsInvalidName() {
        // Arrange
        UUID id = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        // Act
        ThrowingCallable blankName = () -> new Project(id, "   ", "", teamId, ownerId);
        ThrowingCallable shortName = () -> new Project(id, "ab", "", teamId, ownerId);
        ThrowingCallable longName = () -> new Project(id, "n".repeat(101), "", teamId, ownerId);

        // Assert
        assertThatIllegalArgumentException().isThrownBy(blankName);
        assertThatIllegalArgumentException().isThrownBy(shortName);
        assertThatIllegalArgumentException().isThrownBy(longName);
    }

    @Test
    void rejectsMissingOrOversizedDescriptionAndReferences() {
        // Arrange
        UUID id = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        // Act
        ThrowingCallable missingDescription = () -> new Project(id, "Project", null, teamId, ownerId);
        ThrowingCallable longDescription = () -> new Project(id, "Project", "d".repeat(1001), teamId, ownerId);
        ThrowingCallable missingId = () -> new Project(null, "Project", "", teamId, ownerId);
        ThrowingCallable missingTeam = () -> new Project(id, "Project", "", null, ownerId);
        ThrowingCallable missingOwner = () -> new Project(id, "Project", "", teamId, null);

        // Assert
        assertThatIllegalArgumentException().isThrownBy(missingDescription);
        assertThatIllegalArgumentException().isThrownBy(longDescription);
        assertThatNullPointerException().isThrownBy(missingId).withMessage("id is required");
        assertThatNullPointerException().isThrownBy(missingTeam).withMessage("teamId is required");
        assertThatNullPointerException().isThrownBy(missingOwner).withMessage("ownerId is required");
    }
}
