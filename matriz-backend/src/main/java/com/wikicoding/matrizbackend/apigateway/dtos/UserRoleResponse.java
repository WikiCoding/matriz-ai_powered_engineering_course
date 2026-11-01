package com.wikicoding.matrizbackend.apigateway.dtos;

import java.util.UUID;

public record UserRoleResponse(UUID id, String email, String role) {}
