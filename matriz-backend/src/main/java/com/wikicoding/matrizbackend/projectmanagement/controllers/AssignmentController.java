package com.wikicoding.matrizbackend.projectmanagement.controllers;

import com.wikicoding.matrizbackend.projectmanagement.services.AssignmentService;
import com.wikicoding.matrizbackend.projectmanagement.services.AssignmentService.Assignment;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tasks/{taskId}/assignments")
@RequiredArgsConstructor
public class AssignmentController {
    private final AssignmentService assignments;

    @GetMapping
    public List<Assignment> list(@PathVariable UUID taskId) {
        return assignments.list(taskId);
    }

    @PostMapping
    public ResponseEntity<Assignment> add(@PathVariable UUID taskId, @Valid @RequestBody AssignmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(assignments.add(taskId, request.userId()));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> remove(@PathVariable UUID taskId, @PathVariable UUID userId) {
        assignments.remove(taskId, userId);
        return ResponseEntity.noContent().build();
    }

    public record AssignmentRequest(@NotNull UUID userId) {}
}
