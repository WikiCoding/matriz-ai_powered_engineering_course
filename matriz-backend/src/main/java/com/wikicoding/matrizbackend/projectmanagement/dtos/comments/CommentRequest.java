package com.wikicoding.matrizbackend.projectmanagement.dtos.comments;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CommentRequest(@NotNull UUID taskId, @NotBlank @Size(max = 2000) String content) {}
