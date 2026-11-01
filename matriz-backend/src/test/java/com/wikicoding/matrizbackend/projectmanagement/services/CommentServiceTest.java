package com.wikicoding.matrizbackend.projectmanagement.services;

import com.wikicoding.matrizbackend.projectmanagement.ProjectOwnerLookup;
import com.wikicoding.matrizbackend.projectmanagement.dtos.comments.CommentRequest;
import com.wikicoding.matrizbackend.projectmanagement.persistence.task.TaskRepository;
import com.wikicoding.matrizbackend.projectmanagement.persistence.comment.CommentDataModel;
import com.wikicoding.matrizbackend.projectmanagement.persistence.comment.CommentRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CommentServiceTest {
    private final CommentRepository comments = mock(CommentRepository.class);
    private final TaskRepository tasks = mock(TaskRepository.class);
    private final ProjectOwnerLookup authors = mock(ProjectOwnerLookup.class);
    private final CommentService service = new CommentService(comments, tasks, authors);

    @Test
    void listsCommentsForExistingTaskInStableOrder() {
        // Arrange
        UUID taskId = UUID.randomUUID();
        when(tasks.existsById(taskId)).thenReturn(true);
        when(comments.findByTaskId(eq(taskId), any(Sort.class))).thenReturn(List.of());

        // Act
        var result = service.findAllCommentsFromTask(taskId);

        // Assert
        assertThat(result).isEmpty();
        verify(comments).findByTaskId(eq(taskId), argThat(sort ->
                sort.getOrderFor("createdAt") != null && sort.getOrderFor("id") != null));
    }

    @Test
    void findsCommentById() {
        // Arrange
        UUID id = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        when(comments.findById(id)).thenReturn(Optional.of(new CommentDataModel(id, taskId, authorId,
                "Hello", Instant.parse("2026-01-01T00:00:00Z"))));

        // Act
        var result = service.findCommentById(id);

        // Assert
        assertThat(result.getContent()).isEqualTo("Hello");
        assertThat(result.getTaskId()).isEqualTo(taskId);
    }

    @Test
    void createsCommentWithAuthenticatedAuthor() {
        // Arrange
        UUID taskId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        when(tasks.existsById(taskId)).thenReturn(true);
        when(authors.requireActiveOwnerId("subject")).thenReturn(authorId);

        // Act
        var result = service.createComment(new CommentRequest(taskId, "Hello"), "subject");

        // Assert
        assertThat(result.getAuthorId()).isEqualTo(authorId);
        verify(comments).save(argThat(stored -> stored.getTaskId().equals(taskId)
                && stored.getAuthorId().equals(authorId) && stored.getContent().equals("Hello")));
    }

    @Test
    void rejectsMissingTaskWithoutWriting() {
        // Arrange
        UUID taskId = UUID.randomUUID();

        // Act / Assert
        assertThatThrownBy(() -> service.createComment(new CommentRequest(taskId, "Hello"), "subject"))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        error -> assertThat(error.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        verifyNoInteractions(comments, authors);
    }
}
