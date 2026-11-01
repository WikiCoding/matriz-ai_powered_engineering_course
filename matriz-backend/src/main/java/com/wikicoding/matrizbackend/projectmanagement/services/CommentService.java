package com.wikicoding.matrizbackend.projectmanagement.services;

import com.wikicoding.matrizbackend.projectmanagement.ProjectOwnerLookup;
import com.wikicoding.matrizbackend.projectmanagement.domain.comment.Comment;
import com.wikicoding.matrizbackend.projectmanagement.dtos.comments.CommentRequest;
import com.wikicoding.matrizbackend.projectmanagement.persistence.task.TaskRepository;
import com.wikicoding.matrizbackend.projectmanagement.persistence.comment.CommentDataModel;
import com.wikicoding.matrizbackend.projectmanagement.persistence.comment.CommentRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class CommentService {
    private final CommentRepository comments;
    private final TaskRepository tasks;
    private final ProjectOwnerLookup authorLookup;

    @Transactional(readOnly = true)
    public List<Comment> findAllCommentsFromTask(UUID taskId) {
        requireTask(taskId);
        return comments.findByTaskId(taskId, Sort.by(Sort.Order.asc("createdAt"), Sort.Order.asc("id")))
                .stream().map(this::toDomain).toList();
    }

    @Transactional(readOnly = true)
    public Comment findCommentById(UUID commentId) {
        return comments.findById(commentId).map(this::toDomain)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found"));
    }

    @Transactional
    public Comment createComment(CommentRequest request, String subject) {
        requireTask(request.taskId());
        UUID authorId = authorLookup.requireActiveOwnerId(subject);
        Comment comment;
        try {
            comment = new Comment(UUID.randomUUID(), request.taskId(), authorId, request.content(), Instant.now());
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
        comments.save(new CommentDataModel(comment.getId(), comment.getTaskId(), comment.getAuthorId(),
                comment.getContent(), comment.getCreatedAt()));
        return comment;
    }

    private void requireTask(UUID taskId) {
        if (taskId == null || !tasks.existsById(taskId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found");
        }
    }

    private Comment toDomain(CommentDataModel stored) {
        return new Comment(stored.getId(), stored.getTaskId(), stored.getAuthorId(),
                stored.getContent(), stored.getCreatedAt());
    }
}
