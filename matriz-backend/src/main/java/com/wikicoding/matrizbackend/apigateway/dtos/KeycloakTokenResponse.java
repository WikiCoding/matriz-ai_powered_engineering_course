package com.wikicoding.matrizbackend.apigateway.dtos;

public record KeycloakTokenResponse(
        String access_token, String refresh_token, String token_type,
        Long expires_in, Long refresh_expires_in, String scope) { }
