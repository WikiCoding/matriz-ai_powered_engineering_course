package com.wikicoding.matrizbackend.projectmanagement.controllers;

import com.wikicoding.matrizbackend.projectmanagement.dtos.tasks.TaskResponse;
import com.wikicoding.matrizbackend.projectmanagement.dtos.tasks.TaskRequest;
import com.wikicoding.matrizbackend.projectmanagement.dtos.tasks.TaskUpdateRequest;
import com.wikicoding.matrizbackend.projectmanagement.services.TaskService;
import com.wikicoding.matrizbackend.projectmanagement.services.CommentService;
import com.wikicoding.matrizbackend.projectmanagement.dtos.comments.CommentResponse;
import java.util.List;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class TaskController {
    private final TaskService taskService;
    private final CommentService commentService;

    @GetMapping("/tasks/{taskId}/comments")
    public List<CommentResponse> findAllCommentsFromTask(@PathVariable UUID taskId) {
        return commentService.findAllCommentsFromTask(taskId).stream().map(CommentResponse::from).toList();
    }

    @PostMapping("/tasks")
    public ResponseEntity<TaskResponse> createTask(@Valid @RequestBody TaskRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(TaskResponse.from(taskService.createTask(request)));
    }

    @GetMapping("/tasks/{taskId}")
    public TaskResponse findTaskById(@PathVariable UUID taskId) {
        return TaskResponse.from(taskService.findTaskById(taskId));
    }

    @PatchMapping("/tasks/{taskId}")
    public TaskResponse updateTask(@PathVariable UUID taskId, @Valid @RequestBody TaskUpdateRequest request) {
        return TaskResponse.from(taskService.updateTask(taskId, request));
    }

    @DeleteMapping("/tasks/{taskId}")
    public ResponseEntity<Void> deleteTask(@PathVariable UUID taskId) {
        taskService.deleteTask(taskId);
        return ResponseEntity.noContent().build();
    }
}
