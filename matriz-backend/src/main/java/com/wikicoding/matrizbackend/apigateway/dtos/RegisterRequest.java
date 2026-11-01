package com.wikicoding.matrizbackend.apigateway.dtos;

import java.util.UUID;

public record RegisterRequest(String username, String email, String firstName, String lastName,
                              String password, String role, UUID teamId) { }
