package com.wikicoding.matrizbackend.user.dtos;

import java.util.UUID;

public record Profile(UUID id, String displayName, String email, UUID teamId, String role) {}
