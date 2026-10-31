package com.wikicoding.matrizbackend.apigateway.controllers;

import java.util.UUID;

public record RegisterRequest(String username, String email, String password, String role, UUID teamId) { }
