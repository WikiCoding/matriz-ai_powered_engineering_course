package com.wikicoding.matrizbackend.team.controllers;

import com.wikicoding.matrizbackend.team.services.TeamInUseException;
import com.wikicoding.matrizbackend.team.services.TeamNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Maps domain failures from HTTP controllers to the API error envelope. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(TeamNotFoundException.class)
    ResponseEntity<ErrorResponse> handleTeamNotFound(TeamNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, "TEAM_NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler(TeamInUseException.class)
    ResponseEntity<ErrorResponse> handleTeamInUse(TeamInUseException exception) {
        return error(HttpStatus.CONFLICT, "TEAM_IN_USE", exception.getMessage());
    }

    private ResponseEntity<ErrorResponse> error(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(new ErrorResponse(code, message));
    }

    public record ErrorResponse(String code, String message) {
    }
}
