package com.wikicoding.matrizbackend.apigateway;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.MethodArgumentNotValidException;

/** Maps gateway controller failures to the existing API error envelope. */
@RestControllerAdvice(basePackages = {"com.wikicoding.matrizbackend.apigateway.controllers",
        "com.wikicoding.matrizbackend.user.controllers"})
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        return ResponseEntity.badRequest().body(new ErrorResponse("BAD_REQUEST",
                "A valid email and all required fields must be provided"));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(ResponseStatusException exception) {
        HttpStatus status = HttpStatus.resolve(exception.getStatusCode().value());
        String code = status == null ? "HTTP_" + exception.getStatusCode().value() : status.name();
        String message = exception.getReason() == null ? "Request failed" : exception.getReason();
        return ResponseEntity.status(exception.getStatusCode()).body(new ErrorResponse(code, message));
    }

    public record ErrorResponse(String code, String message) {}
}
