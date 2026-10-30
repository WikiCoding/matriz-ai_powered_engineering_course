package com.wikicoding.matrizapigateway.controllers;

import java.util.UUID;

public record RegisterRequest(String username, String email, String password, String role, UUID teamId) { }
