package com.wikicoding.matrizbackend.projectmanagement.domain;

import com.wikicoding.matrizbackend.projectmanagement.domain.comment.Comment;
import org.junit.jupiter.api.Test;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class CommentTest {

    @Test
    void createsCommentWithMaximumLengthContent() {
        // Arrange
        UUID id = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-10-30T10:15:30Z");
        String content = "c".repeat(2000);

        // Act
        Comment comment = new Comment(id, taskId, authorId, content, createdAt);

        // Assert
        assertThat(comment.getId()).isEqualTo(id);
        assertThat(comment.getTaskId()).isEqualTo(taskId);
        assertThat(comment.getAuthorId()).isEqualTo(authorId);
        assertThat(comment.getContent()).isEqualTo(content);
        assertThat(comment.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    void rejectsBlankOrOversizedContent() {
        // Arrange
        UUID id = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-10-30T10:15:30Z");

        // Act
        ThrowingCallable missingContent = () -> new Comment(id, taskId, authorId, null, createdAt);
        ThrowingCallable blankContent = () -> new Comment(id, taskId, authorId, " \n", createdAt);
        ThrowingCallable longContent = () -> new Comment(id, taskId, authorId, "c".repeat(2001), createdAt);

        // Assert
        assertThatIllegalArgumentException().isThrownBy(missingContent);
        assertThatIllegalArgumentException().isThrownBy(blankContent);
        assertThatIllegalArgumentException().isThrownBy(longContent);
    }

    @Test
    void rejectsMissingReferencesAndTimestamp() {
        // Arrange
        UUID id = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-10-30T10:15:30Z");

        // Act
        ThrowingCallable missingId = () -> new Comment(null, taskId, authorId, "Comment", createdAt);
        ThrowingCallable missingTask = () -> new Comment(id, null, authorId, "Comment", createdAt);
        ThrowingCallable missingAuthor = () -> new Comment(id, taskId, null, "Comment", createdAt);
        ThrowingCallable missingCreatedAt = () -> new Comment(id, taskId, authorId, "Comment", null);

        // Assert
        assertThatNullPointerException().isThrownBy(missingId).withMessage("id is required");
        assertThatNullPointerException().isThrownBy(missingTask).withMessage("taskId is required");
        assertThatNullPointerException().isThrownBy(missingAuthor).withMessage("authorId is required");
        assertThatNullPointerException().isThrownBy(missingCreatedAt).withMessage("createdAt is required");
    }
}
