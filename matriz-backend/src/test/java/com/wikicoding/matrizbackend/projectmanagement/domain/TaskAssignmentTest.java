package com.wikicoding.matrizbackend.projectmanagement.domain;

import org.junit.jupiter.api.Test;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class TaskAssignmentTest {

    @Test
    void createsAssignmentForTaskAndUser() {
        // Arrange
        UUID taskId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        // Act
        TaskAssignment assignment = new TaskAssignment(taskId, userId);

        // Assert
        assertThat(assignment.getTaskId()).isEqualTo(taskId);
        assertThat(assignment.getUserId()).isEqualTo(userId);
    }

    @Test
    void rejectsMissingTaskOrUser() {
        // Arrange
        UUID taskId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        // Act
        ThrowingCallable missingTask = () -> new TaskAssignment(null, userId);
        ThrowingCallable missingUser = () -> new TaskAssignment(taskId, null);

        // Assert
        assertThatNullPointerException().isThrownBy(missingTask).withMessage("taskId is required");
        assertThatNullPointerException().isThrownBy(missingUser).withMessage("userId is required");
    }
}
