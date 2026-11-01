package com.wikicoding.matrizbackend.projectmanagement.domain;

import com.wikicoding.matrizbackend.projectmanagement.domain.task.Priority;
import com.wikicoding.matrizbackend.projectmanagement.domain.task.Status;
import com.wikicoding.matrizbackend.projectmanagement.domain.task.Task;
import org.junit.jupiter.api.Test;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class TaskTest {

    @Test
    void createsTaskWithAllowedEffortAndBoundaryLengthFields() {
        // Arrange
        UUID id = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-10-30T10:15:30Z");
        Instant updatedAt = Instant.parse("2026-10-30T10:20:30Z");
        String title = "t".repeat(150);
        String description = "d".repeat(2000);

        // Act
        Task task = new Task(id, projectId, title, description, (short) 13,
                Status.IN_PROGRESS, Priority.HIGH, createdAt, updatedAt);

        // Assert
        assertThat(task.getId()).isEqualTo(id);
        assertThat(task.getProjectId()).isEqualTo(projectId);
        assertThat(task.getTitle()).isEqualTo(title);
        assertThat(task.getDescription()).isEqualTo(description);
        assertThat(task.getEffort()).isEqualTo((short) 13);
        assertThat(task.getStatus()).isEqualTo(Status.IN_PROGRESS);
        assertThat(task.getPriority()).isEqualTo(Priority.HIGH);
        assertThat(task.getCreatedAt()).isEqualTo(createdAt);
        assertThat(task.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void rejectsInvalidTitleDescriptionAndEffort() {
        // Arrange
        UUID id = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        Instant timestamp = Instant.parse("2026-10-30T10:15:30Z");

        // Act
        ThrowingCallable blankTitle = () -> createTask(id, projectId, " \t", "", (short) 3, timestamp, timestamp);
        ThrowingCallable shortTitle = () -> createTask(id, projectId, "ab", "", (short) 3, timestamp, timestamp);
        ThrowingCallable longTitle = () -> createTask(id, projectId, "t".repeat(151), "", (short) 3, timestamp, timestamp);
        ThrowingCallable missingDescription = () -> createTask(id, projectId, "Task", null, (short) 3, timestamp, timestamp);
        ThrowingCallable longDescription = () -> createTask(id, projectId, "Task", "d".repeat(2001), (short) 3, timestamp, timestamp);
        ThrowingCallable invalidEffort = () -> createTask(id, projectId, "Task", "", (short) 4, timestamp, timestamp);

        // Assert
        assertThatIllegalArgumentException().isThrownBy(blankTitle);
        assertThatIllegalArgumentException().isThrownBy(shortTitle);
        assertThatIllegalArgumentException().isThrownBy(longTitle);
        assertThatIllegalArgumentException().isThrownBy(missingDescription);
        assertThatIllegalArgumentException().isThrownBy(longDescription);
        assertThatIllegalArgumentException().isThrownBy(invalidEffort)
                .withMessage("effort must be one of 1, 2, 3, 5, 8, 13");
    }

    @Test
    void rejectsMissingRequiredReferencesEnumsAndTimestamps() {
        // Arrange
        UUID id = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        Instant timestamp = Instant.parse("2026-10-30T10:15:30Z");

        // Act
        ThrowingCallable missingId = () -> createTask(null, projectId, "Task", "", (short) 3, timestamp, timestamp);
        ThrowingCallable missingProject = () -> createTask(id, null, "Task", "", (short) 3, timestamp, timestamp);
        ThrowingCallable missingStatus = () -> new Task(id, projectId, "Task", "", (short) 3,
                null, Priority.MEDIUM, timestamp, timestamp);
        ThrowingCallable missingPriority = () -> new Task(id, projectId, "Task", "", (short) 3,
                Status.TODO, null, timestamp, timestamp);
        ThrowingCallable missingCreatedAt = () -> createTask(id, projectId, "Task", "", (short) 3, null, timestamp);
        ThrowingCallable missingUpdatedAt = () -> createTask(id, projectId, "Task", "", (short) 3, timestamp, null);

        // Assert
        assertThatNullPointerException().isThrownBy(missingId).withMessage("id is required");
        assertThatNullPointerException().isThrownBy(missingProject).withMessage("projectId is required");
        assertThatNullPointerException().isThrownBy(missingStatus).withMessage("status is required");
        assertThatNullPointerException().isThrownBy(missingPriority).withMessage("priority is required");
        assertThatNullPointerException().isThrownBy(missingCreatedAt).withMessage("createdAt is required");
        assertThatNullPointerException().isThrownBy(missingUpdatedAt).withMessage("updatedAt is required");
    }

    private Task createTask(UUID id, UUID projectId, String title, String description, short effort,
                            Instant createdAt, Instant updatedAt) {
        return new Task(id, projectId, title, description, effort, Status.TODO,
                Priority.MEDIUM, createdAt, updatedAt);
    }
}
