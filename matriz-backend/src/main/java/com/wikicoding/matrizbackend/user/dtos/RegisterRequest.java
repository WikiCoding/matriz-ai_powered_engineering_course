package com.wikicoding.matrizbackend.user.dtos;

import java.util.UUID;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RegisterRequest(@NotBlank @Email String email, String firstName, String lastName,
                              String password, String role, UUID teamId) { }
