package com.wikicoding.matrizbackend.user.dtos;

import java.util.UUID;

public record UserItem(UUID id, String email, UUID teamId) {}
