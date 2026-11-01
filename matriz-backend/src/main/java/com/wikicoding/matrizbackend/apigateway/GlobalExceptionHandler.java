package com.wikicoding.matrizbackend.apigateway;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

/** Maps gateway controller failures to the existing API error envelope. */
@RestControllerAdvice(basePackages = {"com.wikicoding.matrizbackend.apigateway.controllers",
        "com.wikicoding.matrizbackend.user.controllers"})
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(ResponseStatusException exception) {
        HttpStatus status = HttpStatus.resolve(exception.getStatusCode().value());
        String code = status == null ? "HTTP_" + exception.getStatusCode().value() : status.name();
        String message = exception.getReason() == null ? "Request failed" : exception.getReason();
        return ResponseEntity.status(exception.getStatusCode()).body(new ErrorResponse(code, message));
    }

    public record ErrorResponse(String code, String message) {}
}
