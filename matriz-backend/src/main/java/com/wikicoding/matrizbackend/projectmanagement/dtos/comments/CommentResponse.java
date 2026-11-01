package com.wikicoding.matrizbackend.projectmanagement.dtos.comments;

import com.wikicoding.matrizbackend.projectmanagement.domain.comment.Comment;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public record CommentResponse(UUID id, UUID taskId, UUID authorId, String content, String createdAt) {
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    public static CommentResponse from(Comment comment) {
        return new CommentResponse(comment.getId(), comment.getTaskId(), comment.getAuthorId(),
                comment.getContent(), FORMAT.format(comment.getCreatedAt()));
    }
}
