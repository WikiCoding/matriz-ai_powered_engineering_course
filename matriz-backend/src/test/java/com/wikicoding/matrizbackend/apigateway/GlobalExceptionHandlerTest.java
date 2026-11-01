package com.wikicoding.matrizbackend.apigateway;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    @Test
    void mapsGatewayStatusWithoutExposingCause() {
        // Arrange
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        ResponseStatusException exception = new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE, "Role change may require reconciliation",
                new IllegalStateException("provider secret"));

        // Act
        var response = handler.handleResponseStatus(exception);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isEqualTo(new GlobalExceptionHandler.ErrorResponse(
                "SERVICE_UNAVAILABLE", "Role change may require reconciliation"));
    }
}
