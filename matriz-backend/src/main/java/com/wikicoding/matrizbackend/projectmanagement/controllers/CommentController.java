package com.wikicoding.matrizbackend.projectmanagement.controllers;

import com.wikicoding.matrizbackend.projectmanagement.dtos.comments.CommentRequest;
import com.wikicoding.matrizbackend.projectmanagement.dtos.comments.CommentResponse;
import com.wikicoding.matrizbackend.projectmanagement.services.CommentService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/comments")
public class CommentController {
    private final CommentService commentService;

    @GetMapping("/{commentId}")
    public CommentResponse findById(@PathVariable UUID commentId) {
        return CommentResponse.from(commentService.findCommentById(commentId));
    }

    @PostMapping
    public ResponseEntity<CommentResponse> createComment(@Valid @RequestBody CommentRequest request,
            JwtAuthenticationToken principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CommentResponse.from(commentService.createComment(request, principal.getName())));
    }
}
