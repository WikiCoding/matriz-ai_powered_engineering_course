package com.wikicoding.matrizbackend.projectmanagement.domain;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** A comment authored on a task. */
@Getter
@EqualsAndHashCode
@ToString
public class Comment {

    private final UUID id;
    private final UUID taskId;
    private final UUID authorId;
    private final String content;
    private final Instant createdAt;

    public Comment(UUID id, UUID taskId, UUID authorId, String content, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id is required");
        this.taskId = Objects.requireNonNull(taskId, "taskId is required");
        this.authorId = Objects.requireNonNull(authorId, "authorId is required");
        if (content == null || content.isBlank() || content.length() > 2000) {
            throw new IllegalArgumentException("content must be non-blank and at most 2000 characters");
        }
        this.content = content;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt is required");
    }
}
