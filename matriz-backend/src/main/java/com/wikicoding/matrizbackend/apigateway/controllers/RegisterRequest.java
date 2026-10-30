package com.wikicoding.matrizbackend.edge.controllers;

import java.util.UUID;

public record RegisterRequest(String username, String email, String password, String role, UUID teamId) { }
